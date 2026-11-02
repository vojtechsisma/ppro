package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.DomainException;
import cz.uhk.ppro.horyavoda.domain.exception.DoubleBookingException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.domain.model.Category;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.domain.model.Rental;
import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.domain.model.ReservationStatus;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rezervace typu: zákazník chce „lyže 170“, rezervace drží kapacitu kategorie a velikosti.
 * Konkrétní kusy se přiřadí při výdeji, kdy z rezervace vznikne výpůjčka.
 * Rezervace, kterou si nikdo nevyzvedl do 10:00 (+ 30 min), propadne a kapacitu uvolní.
 */
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AvailabilityService availabilityService;
    private final RentalService rentalService;
    private final CustomerService customerService;
    private final EquipmentService equipmentService;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            AvailabilityService availabilityService,
            RentalService rentalService,
            CustomerService customerService,
            EquipmentService equipmentService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.availabilityService = availabilityService;
        this.rentalService = rentalService;
        this.customerService = customerService;
        this.equipmentService = equipmentService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Reservation> listReservations() {
        return reservationRepository.findAllByStart();
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Rezervace s ID " + reservationId + " nebyla nalezena."));
    }

    /** Rezervace typu na termín; v termínu musí zbýt dost volných kusů toho typu. */
    public Reservation createReservation(
            long customerId,
            long categoryId,
            String size,
            int quantity,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd
    ) {
        Customer customer = customerService.getCustomer(customerId);
        Category category = equipmentService.getCategory(categoryId);
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = new Reservation(customer, category, size, quantity, plannedStart, plannedEnd, now);

        long free = availabilityService.freeCount(category, size, plannedStart, plannedEnd, now, null, Set.of());
        if (free < quantity) {
            throw new DoubleBookingException("Nelze rezervovat: " + category.getName() + " " + reservation.getSize()
                    + " je v tomto termínu volných jen " + Math.max(free, 0) + " ks.");
        }
        return reservationRepository.save(reservation);
    }

    /** Výdej na pultu: přiřadí konkrétní kusy a založí výpůjčku. */
    public Reservation pickUp(long reservationId, List<Long> equipmentItemIds) {
        Reservation reservation = getReservation(reservationId);
        LocalDateTime now = LocalDateTime.now(clock);
        reservation.ensurePickableAt(now);

        Set<Long> distinctIds = new LinkedHashSet<>(equipmentItemIds);
        if (distinctIds.size() != reservation.getQuantity()) {
            throw new DomainException("Rezervace je na " + reservation.getQuantity() + " ks, vybráno je "
                    + distinctIds.size() + ".");
        }
        Rental rental = rentalService.bookForReservation(reservation, List.copyOf(distinctIds), now);
        reservation.fulfil(rental);
        return reservation;
    }

    public Reservation cancel(long reservationId) {
        Reservation reservation = getReservation(reservationId);
        reservation.cancel();
        return reservation;
    }

    /** Označí propadlé rezervace; kapacitu uvolňují už od okamžiku propadnutí, úloha jen dorovná stav. */
    public int expireOverdueReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Reservation> expired = reservationRepository.findByStatus(ReservationStatus.ACTIVE).stream()
                .filter(reservation -> reservation.isExpiredAt(now))
                .toList();
        expired.forEach(Reservation::expire);
        return expired.size();
    }
}
