package com.arms.dto;

import jakarta.validation.constraints.Size;

/** Apartment endpoints (SRS FR-1.1). */
public final class ApartmentDtos {

    private ApartmentDtos() {
    }

    public record CreateApartmentRequest(
            @jakarta.validation.constraints.NotBlank @Size(max = 100) String name,
            @Size(max = 120) String location,
            @Size(max = 255) String address,
            @Size(max = 500) String description) {
    }

    public record UpdateApartmentRequest(
            @jakarta.validation.constraints.NotBlank @Size(max = 100) String name,
            @Size(max = 120) String location,
            @Size(max = 255) String address,
            @Size(max = 500) String description) {
    }

    public record ApartmentResponse(
            Long id,
            String name,
            String location,
            String address,
            String description,
            boolean active) {
    }
}
