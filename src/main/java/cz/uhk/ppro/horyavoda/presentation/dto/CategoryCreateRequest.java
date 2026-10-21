package cz.uhk.ppro.horyavoda.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CategoryCreateRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 2, max = 20) String code,
        @NotNull @Positive BigDecimal baseDailyRate,
        @NotNull @Positive BigDecimal weekendRate,
        @Size(max = 1000) String description
) {
}
