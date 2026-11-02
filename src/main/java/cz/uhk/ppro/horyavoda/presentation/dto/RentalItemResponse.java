package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.domain.model.RentalItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalItemResponse(
        Long id,
        Long equipmentItemId,
        String inventoryNumber,
        BigDecimal unitDailyPrice,
        LocalDateTime actualReturnedAt,
        BigDecimal lateFee,
        RentalItemStatus status
) {

    public static RentalItemResponse from(RentalItem item) {
        return new RentalItemResponse(
                item.getId(), item.getEquipmentItem().getId(), item.getEquipmentItem().getInventoryNumber(),
                item.getUnitDailyPrice(), item.getActualReturnedAt(), item.getLateFee(), item.getStatus());
    }
}
