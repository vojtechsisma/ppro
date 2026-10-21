package cz.uhk.ppro.horyavoda.presentation.dto;

import cz.uhk.ppro.horyavoda.domain.model.User;
import cz.uhk.ppro.horyavoda.domain.model.UserRole;

import java.time.LocalDateTime;

/** Veřejný profil uživatele – nikdy neobsahuje hash hesla. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        UserRole role,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
