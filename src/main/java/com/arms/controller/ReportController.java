package com.arms.controller;

import com.arms.dto.RentalDtos.RentalResponse;
import com.arms.dto.ReportDtos.DashboardResponse;
import com.arms.dto.ReportDtos.OccupancyReportResponse;
import com.arms.dto.ReportDtos.RevenueReportResponse;
import com.arms.service.RentalService;
import com.arms.service.ReportPdfService;
import com.arms.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting endpoints (SRS FR-6.1 to FR-6.5).
 *
 * <p>The dashboard is shown to every user on signing in (SRS 3.1); the
 * reports themselves are an administrator function (SRS 4.6).</p>
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;
    private final RentalService rentalService;

    public ReportController(ReportService reportService,
                            ReportPdfService reportPdfService,
                            RentalService rentalService) {
        this.reportService = reportService;
        this.reportPdfService = reportPdfService;
        this.rentalService = rentalService;
    }

    /** FR-6.5: today's occupancy and the number of rooms under maintenance. */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return reportService.dashboard();
    }

    /** FR-6.1: occupancy report for a period selected by the user. */
    @GetMapping("/reports/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public OccupancyReportResponse occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.occupancy(from, to);
    }

    /** FR-6.2: revenue report totalling payments received, grouped by apartment. */
    @GetMapping("/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueReportResponse revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenue(from, to);
    }

    /** FR-6.3: active and past rentals for a selected tenant or room. */
    @GetMapping("/reports/rentals")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RentalResponse> rentals(@RequestParam(required = false) Long tenantId,
                                        @RequestParam(required = false) Long roomId) {
        return rentalService.search(tenantId, roomId, null);
    }

    /** FR-6.4: export the occupancy report as a PDF document. */
    @GetMapping("/reports/occupancy.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> occupancyPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = reportPdfService.occupancyPdf(reportService.occupancy(from, to));
        return pdfResponse(pdf, "occupancy-report.pdf");
    }

    /** FR-6.4: export the revenue report as a PDF document. */
    @GetMapping("/reports/revenue.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> revenuePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = reportPdfService.revenuePdf(reportService.revenue(from, to));
        return pdfResponse(pdf, "revenue-report.pdf");
    }

    private static ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
