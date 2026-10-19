package com.arms.dto;

import com.arms.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** User administration endpoints (SRS FR-7.4). */
public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 120) String fullName,
            @NotNull UserRole role) {
    }

    public record UpdateUserRequest(
            @Size(max = 120) String fullName,
            @NotNull UserRole role) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /** Never exposes password material (FR-7.2). */
    public record UserResponse(
            Long id,
            String username,
            String fullName,
            String role,
            boolean enabled,
            boolean mustChangePassword,
            LocalDateTime createdAt) {
    }
}
