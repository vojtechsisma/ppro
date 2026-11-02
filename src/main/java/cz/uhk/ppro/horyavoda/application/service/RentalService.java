package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.DoubleBookingException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.Rental;
import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Výpůjčky: sjednání (se snapshotem ceny), kontrola překryvu termínů, servisní blokace
 * a kapacity držené rezervacemi typu, vracení po jednotlivých kusech s penále
 * za každý započatý den prodlení.
 */
@Service
@Transactional
public class RentalService {

    private static final DateTimeFormatter TERM_FORMAT = DateTimeFormatter.ofPattern("d. M. HH:mm");

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final AvailabilityService availabilityService;
    private final CustomerService customerService;
    private final Clock clock;

    public RentalService(
            RentalRepository rentalRepository,
            RentalItemRepository rentalItemRepository,
            EquipmentItemRepository equipmentItemRepository,
            AvailabilityService availabilityService,
            CustomerService customerService,
            Clock clock
    ) {
        this.rentalRepository = rentalRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.equipmentItemRepository = equipmentItemRepository;
        this.availabilityService = availabilityService;
        this.customerService = customerService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Rental> listRentals(Long equipmentItemId) {
        return equipmentItemId == null
                ? rentalRepository.findAllNewestFirst()
                : rentalRepository.findByEquipmentItemId(equipmentItemId);
    }

    @Transactional(readOnly = true)
    public Rental getRental(long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Výpůjčka s ID " + rentalId + " nebyla nalezena."));
    }

    /**
     * Sjedná výpůjčku konkrétních kusů na termín [plannedStart, plannedEnd).
     * Začíná-li výpůjčka nejpozději teď, kusy se rovnou vydávají.
     */
    public Rental createRental(
            long customerId,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd,
            List<Long> equipmentItemIds,
            BigDecimal depositAmount,
            BigDecimal discountAmount
    ) {
        Customer customer = customerService.getCustomer(customerId);
        return book(customer, plannedStart, plannedEnd, equipmentItemIds, depositAmount, discountAmount, null);
    }

    /**
     * Výdej rezervace typu: konkrétní kusy se přiřadí teď, výpůjčka běží od výdeje
     * do konce rezervace a přebírá cenu sjednanou při rezervaci.
     */
    Rental bookForReservation(Reservation reservation, List<Long> equipmentItemIds, LocalDateTime now) {
        LocalDateTime start = now.isBefore(reservation.getPlannedEnd()) ? now : reservation.getPlannedStart();
        return book(reservation.getCustomer(), start, reservation.getPlannedEnd(), equipmentItemIds,
                BigDecimal.ZERO, BigDecimal.ZERO, reservation);
    }

    private Rental book(
            Customer customer,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd,
            List<Long> equipmentItemIds,
            BigDecimal depositAmount,
            BigDecimal discountAmount,
            Reservation fromReservation
    ) {
        Rental rental = new Rental(customer, plannedStart, plannedEnd, depositAmount);
        rental.setDiscountAmount(discountAmount);

        Set<Long> requestedIds = new LinkedHashSet<>(equipmentItemIds);
        List<EquipmentItem> items = equipmentItemRepository.findAllByIdForUpdate(requestedIds);
        if (items.size() != requestedIds.size()) {
            throw new EntityNotFoundException("Některý z vybraných kusů v evidenci není.");
        }
        if (fromReservation != null) {
            items.stream().filter(item -> !fromReservation.isOfType(item)).findFirst().ifPresent(item -> {
                throw new DoubleBookingException("Kus " + item.getInventoryNumber() + " neodpovídá rezervaci ("
                        + fromReservation.getCategory().getName() + " " + fromReservation.getSize() + ").");
            });
        }

        LocalDateTime now = LocalDateTime.now(clock);
        // Snapshot ceny: z rezervace, jinak z ceníku kategorie; pozdější změna ceníku smlouvu neovlivní
        items.forEach(item -> rental.addItem(item, fromReservation != null
                ? fromReservation.getUnitDailyPrice()
                : item.getCategory().getBaseDailyRate()));

        ensureNoOverlap(requestedIds, plannedStart, plannedEnd, now);
        ensureReservationsStillFit(items, plannedStart, plannedEnd, now, fromReservation);

        if (!plannedStart.isAfter(now)) {
            rental.handOverItems();
        }
        return rentalRepository.save(rental);
    }

    /** Jeden kus nesmí být ve dvou výpůjčkách, které se časově překrývají. */
    private void ensureNoOverlap(Set<Long> equipmentItemIds, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        for (RentalItem booking : rentalItemRepository.findAllByEquipmentItemIds(equipmentItemIds)) {
            if (booking.blocksEquipmentBetween(from, to, now)) {
                Rental other = booking.getRental();
                throw new DoubleBookingException("Nelze půjčit: kus " + booking.getEquipmentItem().getInventoryNumber()
                        + " je " + TERM_FORMAT.format(other.getPlannedStart()) + " – " + TERM_FORMAT.format(other.getPlannedEnd())
                        + " ve výpůjčce č. " + other.getId() + ".");
            }
        }
    }

    /** Výpůjčka nesmí sebrat kusy, které v termínu drží rezervace typu pro jiné zákazníky. */
    private void ensureReservationsStillFit(
            List<EquipmentItem> items, LocalDateTime from, LocalDateTime to, LocalDateTime now, Reservation fromReservation
    ) {
        Map<String, List<EquipmentItem>> byType = items.stream().collect(Collectors.groupingBy(
                item -> item.getCategory().getId() + "|" + item.getSize().strip().toLowerCase(Locale.ROOT),
                LinkedHashMap::new, Collectors.toList()));
        Long excludedReservationId = fromReservation != null ? fromReservation.getId() : null;

        for (List<EquipmentItem> group : byType.values()) {
            EquipmentItem sample = group.get(0);
            Set<Long> taken = group.stream().map(EquipmentItem::getId).collect(Collectors.toSet());
            long free = availabilityService.freeCount(
                    sample.getCategory(), sample.getSize(), from, to, now, excludedReservationId, taken);
            if (free < 0) {
                throw new DoubleBookingException("Nelze půjčit: " + sample.getCategory().getName() + " " + sample.getSize()
                        + " jsou v tomto termínu rezervované pro jiné zákazníky.");
            }
        }
    }

    /** Vrácení jednoho kusu; bez zadaného času se bere aktuální čas. */
    public Rental returnItem(long rentalId, long rentalItemId, LocalDateTime returnedAt) {
        Rental rental = getRental(rentalId);
        rental.returnItem(rentalItemId, returnedAt != null ? returnedAt : LocalDateTime.now(clock));
        return rental;
    }
}
