package com.arms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Room endpoints (SRS FR-1.2 to FR-1.7). */
public final class RoomDtos {

    private RoomDtos() {
    }

    public record CreateRoomRequest(
            @NotNull Long apartmentId,
            @NotBlank @Size(max = 10) String roomNumber,
            @Size(max = 50) String roomType,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal rentAmount) {
    }

    public record UpdateRoomRequest(
            @NotBlank @Size(max = 10) String roomNumber,
            @Size(max = 50) String roomType) {
    }

    /** FR-1.7: the new amount applies only to rentals created after the change. */
    public record UpdateRentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal rentAmount) {
    }

    /** FR-1.4 / FR-1.5: maintenance with reason and explicit confirmation. */
    public record MaintenanceRequest(
            @NotBlank @Size(max = 255) String reason,
            boolean confirm) {
    }

    public record RoomResponse(
            Long id,
            Long apartmentId,
            String apartmentName,
            String roomNumber,
            String roomType,
            String status,
            String maintenanceReason,
            BigDecimal rentAmount,
            boolean active) {
    }
}
