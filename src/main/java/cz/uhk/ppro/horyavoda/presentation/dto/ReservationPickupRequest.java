package cz.uhk.ppro.horyavoda.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Konkrétní kusy, které obsluha při výdeji rezervace přiřadí. */
public record ReservationPickupRequest(@NotEmpty @Size(max = 20) List<@NotNull Long> equipmentItemIds) {
}
