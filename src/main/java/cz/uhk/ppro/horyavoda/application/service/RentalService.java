package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.DoubleBookingException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.Rental;
import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Výpůjčky: sjednání (se snapshotem ceny), kontrola překryvu termínů a servisní blokace,
 * vracení po jednotlivých kusech s penále za každý započatý den prodlení.
 */
@Service
@Transactional
public class RentalService {

    private static final DateTimeFormatter TERM_FORMAT = DateTimeFormatter.ofPattern("d. M. HH:mm");

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final CustomerService customerService;
    private final Clock clock;

    public RentalService(
            RentalRepository rentalRepository,
            RentalItemRepository rentalItemRepository,
            EquipmentItemRepository equipmentItemRepository,
            CustomerService customerService,
            Clock clock
    ) {
        this.rentalRepository = rentalRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.equipmentItemRepository = equipmentItemRepository;
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
     * Kus nesmí být v servisu a nesmí se časově překrývat s jinou výpůjčkou.
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
        Rental rental = new Rental(customer, plannedStart, plannedEnd, depositAmount);
        rental.setDiscountAmount(discountAmount);

        Set<Long> requestedIds = new LinkedHashSet<>(equipmentItemIds);
        List<EquipmentItem> items = equipmentItemRepository.findAllByIdForUpdate(requestedIds);
        if (items.size() != requestedIds.size()) {
            throw new EntityNotFoundException("Některý z vybraných kusů v evidenci není.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<RentalItem> existingBookings = rentalItemRepository.findAllByEquipmentItemIds(requestedIds);
        for (RentalItem booking : existingBookings) {
            if (booking.blocksEquipmentBetween(plannedStart, plannedEnd, now)) {
                Rental other = booking.getRental();
                throw new DoubleBookingException("Nelze půjčit: kus " + booking.getEquipmentItem().getInventoryNumber()
                        + " je " + TERM_FORMAT.format(other.getPlannedStart()) + " – " + TERM_FORMAT.format(other.getPlannedEnd())
                        + " ve výpůjčce č. " + other.getId() + ".");
            }
        }

        // Snapshot ceny z ceníku kategorie; pozdější změna ceníku smlouvu neovlivní
        items.forEach(item -> rental.addItem(item, item.getCategory().getBaseDailyRate()));
        if (!plannedStart.isAfter(now)) {
            rental.handOverItems();
        }
        return rentalRepository.save(rental);
    }

    /** Vrácení jednoho kusu; bez zadaného času se bere aktuální čas. */
    public Rental returnItem(long rentalId, long rentalItemId, LocalDateTime returnedAt) {
        Rental rental = getRental(rentalId);
        rental.returnItem(rentalItemId, returnedAt != null ? returnedAt : LocalDateTime.now(clock));
        return rental;
    }
}
