package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;

import java.time.LocalDateTime;

public record EquipmentResponse(
        Long id,
        Long categoryId,
        String inventoryNumber,
        String modelName,
        String size,
        int yearAcquired,
        WearCondition wearCondition,
        ItemStatus status,
        LocalDateTime createdAt,
        CategoryResponse category
) {

    public static EquipmentResponse from(EquipmentItem item) {
        CategoryResponse category = CategoryResponse.from(item.getCategory());
        return new EquipmentResponse(
                item.getId(), category.id(), item.getInventoryNumber(), item.getModelName(), item.getSize(),
                item.getYearAcquired(), item.getWearCondition(), item.getStatus(), item.getCreatedAt(), category);
    }
}
