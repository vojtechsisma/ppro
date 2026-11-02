package cz.uhk.ppro.horyavoda.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RentalCreateRequest(
        @NotNull Long customerId,
        @NotNull LocalDateTime plannedStart,
        @NotNull LocalDateTime plannedEnd,
        @NotEmpty @Size(max = 20) List<@NotNull Long> equipmentItemIds,
        @DecimalMin("0") BigDecimal depositAmount,
        @DecimalMin("0") BigDecimal discountAmount
) {

    public BigDecimal depositAmountOrZero() {
        return depositAmount != null ? depositAmount : BigDecimal.ZERO;
    }

    public BigDecimal discountAmountOrZero() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }
}
