package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(min = 2, max = 50) String inventoryNumber,
        @NotBlank @Size(min = 2, max = 100) String modelName,
        @NotBlank @Size(min = 1, max = 30) String size,
        @NotNull @Min(2000) @Max(2100) Integer yearAcquired,
        WearCondition wearCondition,
        ItemStatus status
) {

    public WearCondition wearConditionOrDefault() {
        return wearCondition != null ? wearCondition : WearCondition.GOOD;
    }

    public ItemStatus statusOrDefault() {
        return status != null ? status : ItemStatus.AVAILABLE;
    }
}
