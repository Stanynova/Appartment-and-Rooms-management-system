package com.arms.service;

import com.arms.dto.ReportDtos.DashboardResponse;
import com.arms.dto.ReportDtos.OccupancyReportResponse;
import com.arms.dto.ReportDtos.RevenueReportResponse;
import com.arms.dto.ReportDtos.RevenueReportResponse.RevenueRow;
import com.arms.entity.PaymentStatus;
import com.arms.entity.RoomStatus;
import com.arms.entity.Rental;
import com.arms.entity.RentalStatus;
import com.arms.exception.BadRequestException;
import com.arms.repository.PaymentRepository;
import com.arms.repository.RentalRepository;
import com.arms.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Occupancy and revenue reporting (SRS FR-6.1, FR-6.2, FR-6.5).
 *
 * <p>Occupancy for a period is computed from the rentals that cover it:
 * occupied room-days divided by rentable room-days. The percentage is
 * computed in fixed-point decimal arithmetic (NFR-QA-4).</p>
 */
@Service
public class ReportService {

    private final RoomRepository rooms;
    private final RentalRepository rentals;
    private final PaymentRepository payments;
    private final Messages messages;

    public ReportService(RoomRepository rooms,
                         RentalRepository rentals,
                         PaymentRepository payments,
                         Messages messages) {
        this.rooms = rooms;
        this.rentals = rentals;
        this.payments = payments;
        this.messages = messages;
    }

    /** FR-6.1: rooms available, rooms occupied and occupancy for a period. */
    @Transactional(readOnly = true)
    public OccupancyReportResponse occupancy(LocalDate from, LocalDate to) {
        LocalDate start = resolveFrom(from);
        LocalDate end = resolveTo(to);
        ensureOrdered(start, end);

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int totalRooms = (int) rooms.countByActiveTrue();
        long occupiedRoomDays = occupiedRoomDays(start, end);
        long totalRoomDays = totalRooms * days;

        return new OccupancyReportResponse(
                start,
                end,
                totalRooms,
                days,
                occupiedRoomDays,
                totalRoomDays,
                percentage(occupiedRoomDays, totalRoomDays),
                (int) rooms.countByActiveTrueAndStatus(RoomStatus.AVAILABLE),
                (int) rooms.countByActiveTrueAndStatus(RoomStatus.OCCUPIED),
                (int) rooms.countByActiveTrueAndStatus(RoomStatus.UNDER_MAINTENANCE));
    }

    /** FR-6.2: payments received in a period, grouped by apartment. */
    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(LocalDate from, LocalDate to) {
        LocalDate start = resolveFrom(from);
        LocalDate end = resolveTo(to);
        ensureOrdered(start, end);

        List<RevenueRow> rows = payments
                .sumRevenueByApartment(start, end, PaymentStatus.PAID)
                .stream()
                .map(row -> new RevenueRow((Long) row[0], (String) row[1],
                        row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]))
                .toList();
        BigDecimal grandTotal = rows.stream()
                .map(RevenueRow::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RevenueReportResponse(start, end, rows, grandTotal);
    }

    /** FR-6.5: dashboard summarising today's occupancy and maintenance. */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        int total = (int) rooms.countByActiveTrue();
        int occupied = (int) rooms.countByActiveTrueAndStatus(RoomStatus.OCCUPIED);
        return new DashboardResponse(
                LocalDate.now(),
                total,
                (int) rooms.countByActiveTrueAndStatus(RoomStatus.AVAILABLE),
                occupied,
                (int) rooms.countByActiveTrueAndStatus(RoomStatus.UNDER_MAINTENANCE),
                percentage(occupied, total),
                rentals.countByStatus(RentalStatus.ACTIVE));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Sum the days of the period covered by ACTIVE or COMPLETED rentals, merged per room. */
    private long occupiedRoomDays(LocalDate from, LocalDate to) {
        List<Rental> overlapping = rentals.findOverlappingPeriod(from, to,
                List.of(RentalStatus.ACTIVE, RentalStatus.COMPLETED));

        Map<Long, List<long[]>> byRoom = overlapping.stream()
                .collect(Collectors.groupingBy(
                        rental -> rental.getRoom().getId(),
                        Collectors.mapping(rental -> new long[]{
                                Math.max(rental.getStartDate().toEpochDay(), from.toEpochDay()),
                                Math.min(rental.getEndDate().toEpochDay(), to.toEpochDay())
                        }, Collectors.toList())));

        long total = 0;
        for (List<long[]> intervals : byRoom.values()) {
            intervals.sort(Comparator.comparingLong(interval -> interval[0]));
            long currentStart = intervals.get(0)[0];
            long currentEnd = intervals.get(0)[1];
            for (int i = 1; i < intervals.size(); i++) {
                long start = intervals.get(i)[0];
                long end = intervals.get(i)[1];
                if (start <= currentEnd + 1) {
                    currentEnd = Math.max(currentEnd, end);
                } else {
                    total += currentEnd - currentStart + 1;
                    currentStart = start;
                    currentEnd = end;
                }
            }
            total += currentEnd - currentStart + 1;
        }
        return total;
    }

    private static BigDecimal percentage(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private static LocalDate resolveFrom(LocalDate from) {
        if (from != null) {
            return from;
        }
        return YearMonth.now().atDay(1);
    }

    private static LocalDate resolveTo(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }

    private void ensureOrdered(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException(messages.get("common.invalidPeriod"), "INVALID_PERIOD");
        }
    }
}
