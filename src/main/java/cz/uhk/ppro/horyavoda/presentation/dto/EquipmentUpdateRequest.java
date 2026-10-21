package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;
import jakarta.validation.constraints.Size;

/** Částečná úprava kusu – null hodnoty se ponechají beze změny. */
public record EquipmentUpdateRequest(
        @Size(min = 2, max = 100) String modelName,
        @Size(min = 1, max = 30) String size,
        WearCondition wearCondition,
        ItemStatus status
) {
}
