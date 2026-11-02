package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long customerId,
        String customerName,
        Long categoryId,
        String categoryName,
        String size,
        int quantity,
        LocalDateTime plannedStart,
        LocalDateTime plannedEnd,
        LocalDateTime pickupDeadline,
        LocalDateTime holdsUntil,
        BigDecimal unitDailyPrice,
        ReservationStatus status,
        Long rentalId,
        LocalDateTime createdAt
) {

    /** Stav se vyhodnocuje k danému okamžiku – propadlá rezervace je EXPIRED, i když ji úloha ještě neoznačila. */
    public static ReservationResponse from(Reservation reservation, LocalDateTime now) {
        return new ReservationResponse(
                reservation.getId(), reservation.getCustomer().getId(),
                reservation.getCustomer().getLastName() + " " + reservation.getCustomer().getFirstName(),
                reservation.getCategory().getId(), reservation.getCategory().getName(),
                reservation.getSize(), reservation.getQuantity(),
                reservation.getPlannedStart(), reservation.getPlannedEnd(),
                reservation.getPickupDeadline(), reservation.holdsUntil(),
                reservation.getUnitDailyPrice(), reservation.statusAt(now),
                reservation.getRental() != null ? reservation.getRental().getId() : null,
                reservation.getCreatedAt());
    }
}
