package cz.uhk.ppro.horyavoda.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        @NotNull Long customerId,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 30) String size,
        @NotNull @Min(1) @Max(20) Integer quantity,
        @NotNull LocalDateTime plannedStart,
        @NotNull LocalDateTime plannedEnd
) {
}
