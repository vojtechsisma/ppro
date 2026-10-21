package cz.uhk.ppro.horyavoda.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
        @NotBlank @Size(min = 2, max = 50) String firstName,
        @NotBlank @Size(min = 2, max = 50) String lastName,
        @NotBlank @Size(min = 6, max = 20) String phone,
        @NotBlank @Size(min = 4, max = 30) String idDocumentNumber
) {
}
