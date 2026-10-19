package com.arms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reporting endpoints (SRS FR-6.1 to FR-6.5). */
public final class ReportDtos {

    private ReportDtos() {
    }

    /** FR-6.1: occupancy for a user-selected period. */
    public record OccupancyReportResponse(
            LocalDate from,
            LocalDate to,
            int totalRooms,
            long days,
            long occupiedRoomDays,
            long totalRoomDays,
            BigDecimal occupancyPercentage,
            int availableRooms,
            int occupiedRooms,
            int underMaintenanceRooms) {
    }

    /** FR-6.2: revenue for a period grouped by apartment. */
    public record RevenueReportResponse(
            LocalDate from,
            LocalDate to,
            List<RevenueRow> rows,
            BigDecimal grandTotal) {

        public record RevenueRow(
                Long apartmentId,
                String apartmentName,
                BigDecimal total) {
        }
    }

    /** FR-6.5: dashboard summarising today's occupancy and maintenance. */
    public record DashboardResponse(
            LocalDate asOf,
            int totalRooms,
            int availableRooms,
            int occupiedRooms,
            int underMaintenanceRooms,
            BigDecimal occupancyPercentage,
            long activeRentals) {
    }
}
