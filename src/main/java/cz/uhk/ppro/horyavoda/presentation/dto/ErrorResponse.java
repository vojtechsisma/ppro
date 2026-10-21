package cz.uhk.ppro.horyavoda.presentation.dto;

/** Jednotný formát chybové odpovědi API: {@code {"detail": "..."}}. */
public record ErrorResponse(String detail) {
}
