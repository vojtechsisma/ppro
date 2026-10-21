package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(min = 2, max = 100) String fullName,
        UserRole role
) {

    /** Výchozí role nového uživatele je obsluha pultu. */
    public UserRole roleOrDefault() {
        return role != null ? role : UserRole.STAFF;
    }
}
