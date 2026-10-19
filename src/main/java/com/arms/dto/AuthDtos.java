package com.arms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Authentication endpoints (SRS FR-7.1). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 72) String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /** The signed-in account. Never exposes password material (FR-7.2). */
    public record SessionResponse(
            Long id,
            String username,
            String fullName,
            String role,
            boolean mustChangePassword) {
    }
}
