package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.Category;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        BigDecimal baseDailyRate,
        BigDecimal weekendRate,
        String description
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getCode(),
                category.getBaseDailyRate(), category.getWeekendRate(), category.getDescription());
    }
}
