package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.domain.model.RentalItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalItemResponse(
        Long id,
        Long equipmentItemId,
        String inventoryNumber,
        String modelName,
        String categoryName,
        String size,
        BigDecimal unitDailyPrice,
        LocalDateTime actualReturnedAt,
        BigDecimal lateFee,
        RentalItemStatus status
) {

    public static RentalItemResponse from(RentalItem item) {
        EquipmentItem equipment = item.getEquipmentItem();
        return new RentalItemResponse(
                item.getId(), equipment.getId(), equipment.getInventoryNumber(), equipment.getModelName(),
                equipment.getCategory().getName(), equipment.getSize(),
                item.getUnitDailyPrice(), item.getActualReturnedAt(), item.getLateFee(), item.getStatus());
    }
}
