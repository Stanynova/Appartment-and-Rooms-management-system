package com.arms.service;

import com.arms.dto.ReportDtos.OccupancyReportResponse;
import com.arms.dto.ReportDtos.RevenueReportResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * PDF export of reports for printing and record-keeping (SRS FR-6.4).
 * The layout fits A4 paper (SRS 3.1, printing requirement).
 */
@Service
public class ReportPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] occupancyPdf(OccupancyReportResponse report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeading(document, "Occupancy Report",
                    "Period: " + DATE_FORMAT.format(report.from())
                            + " to " + DATE_FORMAT.format(report.to()));

            PdfPTable table = newTable("Metric", "Value");
            addRow(table, "Total rentable rooms", String.valueOf(report.totalRooms()));
            addRow(table, "Rooms available", String.valueOf(report.availableRooms()));
            addRow(table, "Rooms occupied", String.valueOf(report.occupiedRooms()));
            addRow(table, "Rooms under maintenance", String.valueOf(report.underMaintenanceRooms()));
            addRow(table, "Days in period", String.valueOf(report.days()));
            addRow(table, "Occupied room-days", String.valueOf(report.occupiedRoomDays()));
            addRow(table, "Rentable room-days", String.valueOf(report.totalRoomDays()));
            addRow(table, "Occupancy rate", report.occupancyPercentage() + " %");
            document.add(table);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to build the occupancy PDF", e);
        }
    }

    public byte[] revenuePdf(RevenueReportResponse report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeading(document, "Revenue Report",
                    "Period: " + DATE_FORMAT.format(report.from())
                            + " to " + DATE_FORMAT.format(report.to()));

            PdfPTable table = newTable("Apartment", "Revenue (TZS)");
            for (RevenueReportResponse.RevenueRow row : report.rows()) {
                addRow(table, row.apartmentName(), money(row.total()));
            }
            if (report.rows().isEmpty()) {
                addRow(table, "No payments recorded in this period", "");
            }
            document.add(table);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Grand total: " + money(report.grandTotal()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to build the revenue PDF", e);
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void addHeading(Document document, String title, String subtitle)
            throws DocumentException {
        document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)));
        document.add(new Paragraph("Apartment and Room Management System",
                FontFactory.getFont(FontFactory.HELVETICA, 10f)));
        document.add(new Paragraph(subtitle, FontFactory.getFont(FontFactory.HELVETICA, 11f)));
        document.add(Chunk.NEWLINE);
    }

    private static PdfPTable newTable(String firstHeader, String secondHeader) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(headerCell(firstHeader));
        table.addCell(headerCell(secondHeader));
        return table;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f)));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static void addRow(PdfPTable table, String left, String right) {
        table.addCell(left);
        table.addCell(right);
    }

    /** Monetary values carry the TZS code, a thousands separator and two decimals (SRS 7.4). */
    private static String money(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("\"TZS \"#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount);
    }
}
