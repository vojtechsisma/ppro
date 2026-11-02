package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.Rental;
import cz.uhk.ppro.horyavoda.domain.model.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RentalResponse(
        Long id,
        Long customerId,
        String customerName,
        LocalDateTime plannedStart,
        LocalDateTime plannedEnd,
        BigDecimal depositAmount,
        BigDecimal discountAmount,
        BigDecimal totalLateFee,
        RentalStatus status,
        LocalDateTime createdAt,
        List<RentalItemResponse> items
) {

    public static RentalResponse from(Rental rental) {
        return new RentalResponse(
                rental.getId(), rental.getCustomer().getId(),
                rental.getCustomer().getLastName() + " " + rental.getCustomer().getFirstName(),
                rental.getPlannedStart(), rental.getPlannedEnd(),
                rental.getDepositAmount(), rental.getDiscountAmount(), rental.getTotalLateFee(),
                rental.getStatus(), rental.getCreatedAt(),
                rental.getItems().stream().map(RentalItemResponse::from).toList());
    }
}
