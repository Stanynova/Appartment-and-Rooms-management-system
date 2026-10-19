package com.arms.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Rental endpoints (SRS FR-3.1 to FR-3.7). */
public final class RentalDtos {

    private RentalDtos() {
    }

    public record CreateRentalRequest(
            @NotNull Long tenantId,
            @NotNull Long roomId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate) {
    }

    /** FR-3.4: amend the dates of a rental that has not yet started. */
    public record UpdateRentalDatesRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate) {
    }

    /**
     * Rental view including the computed balance (FR-4.3):
     * rent due less the payments recorded against the rental.
     */
    public record RentalResponse(
            Long id,
            Long tenantId,
            String tenantName,
            Long roomId,
            Long apartmentId,
            String apartmentName,
            String roomNumber,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            BigDecimal rentDue,
            BigDecimal totalPaid,
            BigDecimal balanceOutstanding,
            Long createdById,
            String createdByName,
            LocalDateTime createdAt) {
    }
}
