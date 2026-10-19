package com.arms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tenant endpoints (SRS FR-2.1 to FR-2.5). */
public final class TenantDtos {

    private TenantDtos() {
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address) {
    }

    public record UpdateTenantRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address) {
    }

    public record TenantResponse(
            Long id,
            String fullName,
            String phone,
            String email,
            String address) {
    }
}
