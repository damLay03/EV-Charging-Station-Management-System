package com.swp.evchargingstation.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.swp.evchargingstation.dto.response.RevenueReportResponse;
import com.swp.evchargingstation.dto.response.StationRevenueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(102, 126, 234);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    public byte[] exportRevenuePdf(RevenueReportResponse reportData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            Paragraph title = new Paragraph("BÁO CÁO DOANH THU")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(title);

            // Subtitle - Period
            Paragraph subtitle = new Paragraph(reportData.getReportPeriod())
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(3);
            document.add(subtitle);

            // Period details
            Paragraph periodDetails = new Paragraph(reportData.getPeriodDetails())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(periodDetails);

            // Summary section
            addSummarySection(document, reportData.getSummary());

            // Station details table
            if (reportData.getStationDetails() != null && !reportData.getStationDetails().isEmpty()) {
                addStationDetailsTable(document, reportData.getStationDetails());
            }

            // Footer
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            Paragraph footer = new Paragraph(
                    String.format("\nNgày xuất báo cáo: %s",
                            reportData.getGeneratedAt().format(formatter)))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20)
                    .setItalic();
            document.add(footer);

            Paragraph systemFooter = new Paragraph("© 2025 EV Charging Station Management System")
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10);
            document.add(systemFooter);

            document.close();
            log.info("PDF report generated successfully");

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    private void addSummarySection(Document document, RevenueReportResponse.ReportSummary summary) {
        // Summary header
        Paragraph summaryHeader = new Paragraph("TỔNG QUAN")
                .setFontSize(14)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(summaryHeader);

        // Summary table (2 columns)
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Left column
        summaryTable.addCell(createSummaryCell("Tổng doanh thu:",
                formatCurrency(summary.getTotalRevenue()) + " VNĐ", true));
        summaryTable.addCell(createSummaryCell("Tổng số phiên sạc:",
                String.valueOf(summary.getTotalSessions()), false));
        summaryTable.addCell(createSummaryCell("Số trạm có doanh thu:",
                String.valueOf(summary.getTotalStations()), false));

        // Right column
        summaryTable.addCell(createSummaryCell("Doanh thu TB/trạm:",
                formatCurrency(summary.getAverageRevenuePerStation()) + " VNĐ", false));
        summaryTable.addCell(createSummaryCell("Doanh thu TB/phiên:",
                formatCurrency(summary.getAverageRevenuePerSession()) + " VNĐ", false));
        summaryTable.addCell(createSummaryCell("Trạm doanh thu cao nhất:",
                summary.getTopStation() + " (" + formatCurrency(summary.getTopStationRevenue()) + " VNĐ)", false));

        document.add(summaryTable);
    }

    private Cell createSummaryCell(String label, String value, boolean isHighlight) {
        Paragraph content = new Paragraph()
                .add(new Paragraph(label).setBold().setFontSize(10))
                .add(new Paragraph(value).setFontSize(12));

        Cell cell = new Cell()
                .add(content)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);

        if (isHighlight) {
            cell.setBackgroundColor(new DeviceRgb(230, 240, 255));
        }

        return cell;
    }

    private void addStationDetailsTable(Document document, List<StationRevenueResponse> stations) {
        // Details header
        Paragraph detailsHeader = new Paragraph("CHI TIẾT TỪNG TRẠM SẠC")
                .setFontSize(14)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(detailsHeader);

        // Table with 5 columns
        float[] columnWidths = {1f, 3f, 2.5f, 2f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100));

        // Table headers
        table.addHeaderCell(createHeaderCell("STT"));
        table.addHeaderCell(createHeaderCell("Tên trạm"));
        table.addHeaderCell(createHeaderCell("Địa chỉ"));
        table.addHeaderCell(createHeaderCell("Doanh thu (VNĐ)"));
        table.addHeaderCell(createHeaderCell("Số phiên"));

        // Table data
        int index = 1;
        for (StationRevenueResponse station : stations) {
            table.addCell(createDataCell(String.valueOf(index++), TextAlignment.CENTER));
            table.addCell(createDataCell(station.getStationName(), TextAlignment.LEFT));
            table.addCell(createDataCell(station.getAddress(), TextAlignment.LEFT));
            table.addCell(createDataCell(formatCurrency(station.getTotalRevenue()), TextAlignment.RIGHT));
            table.addCell(createDataCell(String.valueOf(station.getTotalSessions()), TextAlignment.CENTER));
        }

        document.add(table);
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(11))
                .setBackgroundColor(HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createDataCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(10))
                .setTextAlignment(alignment)
                .setPadding(6);
    }

    private String formatCurrency(float amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}

