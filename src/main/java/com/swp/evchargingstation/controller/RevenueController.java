package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.RevenueReportResponse;
import com.swp.evchargingstation.dto.response.StationRevenueResponse;
import com.swp.evchargingstation.service.PdfExportService;
import com.swp.evchargingstation.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/revenues")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Revenue Management", description = "RESTful API thống kê doanh thu - Admin only")
public class RevenueController {

    RevenueService revenueService;
    PdfExportService pdfExportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy thống kê doanh thu",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo khoảng thời gian được chỉ định. " +
                    "- daily: cần year, month, day " +
                    "- weekly: cần year, week " +
                    "- monthly: cần year, month " +
                    "- yearly: cần year"
    )
    public ApiResponse<List<StationRevenueResponse>> getRevenue(
            @Parameter(description = "Khoảng thời gian thống kê (daily, weekly, monthly, yearly)", example = "daily")
            @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "Năm (mặc định: năm hiện tại)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tháng (1-12, dùng với period=monthly hoặc daily)", example = "11")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Ngày (1-31, dùng với period=daily)", example = "4")
            @RequestParam(required = false) Integer day,
            @Parameter(description = "Tuần (1-52, dùng với period=weekly)", example = "45")
            @RequestParam(required = false) Integer week) {

        log.info("Admin requesting revenue statistics - period: {}, year: {}, month: {}, day: {}, week: {}",
                period, year, month, day, week);

        // Validate parameters based on period
        String periodLower = period.toLowerCase();

        List<StationRevenueResponse> result = switch (periodLower) {
            case "daily" -> {
                if (year == null || month == null || day == null) {
                    throw new IllegalArgumentException("period=daily requires year, month, and day parameters");
                }
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("month must be between 1 and 12");
                }
                if (day < 1 || day > 31) {
                    throw new IllegalArgumentException("day must be between 1 and 31");
                }
                yield revenueService.getDailyRevenue(year, month, day);
            }
            case "weekly" -> {
                if (year == null || week == null) {
                    throw new IllegalArgumentException("period=weekly requires year and week parameters");
                }
                if (week < 1 || week > 53) {
                    throw new IllegalArgumentException("week must be between 1 and 53");
                }
                yield revenueService.getWeeklyRevenue(year, week);
            }
            case "monthly" -> {
                if (year == null || month == null) {
                    throw new IllegalArgumentException("period=monthly requires year and month parameters");
                }
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("month must be between 1 and 12");
                }
                yield revenueService.getMonthlyRevenue(year, month);
            }
            case "yearly" -> {
                if (year == null) {
                    throw new IllegalArgumentException("period=yearly requires year parameter");
                }
                yield revenueService.getYearlyRevenue(year);
            }
            default -> throw new IllegalArgumentException("Invalid period: " + period + ". Supported: daily, weekly, monthly, yearly");
        };

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xuất báo cáo PDF doanh thu hàng ngày",
            description = "Xuất file PDF báo cáo doanh thu theo ngày cụ thể. Nếu không truyền tham số, lấy ngày hiện tại."
    )
    public ResponseEntity<byte[]> exportDailyReport(
            @Parameter(description = "Năm (mặc định: năm hiện tại)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tháng (1-12, mặc định: tháng hiện tại)", example = "11")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Ngày (1-31, mặc định: ngày hiện tại)", example = "5")
            @RequestParam(required = false) Integer day) {

        // Validate and set defaults
        LocalDate today = LocalDate.now();
        year = year != null ? year : today.getYear();
        month = month != null ? month : today.getMonthValue();
        day = day != null ? day : today.getDayOfMonth();

        log.info("Admin exporting daily revenue report for: {}-{}-{}", year, month, day);

        // Generate report data
        RevenueReportResponse reportData = revenueService.generateDailyReport(year, month, day);

        // Generate PDF
        byte[] pdfBytes = pdfExportService.exportRevenuePdf(reportData);

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(String.format("revenue_report_daily_%04d%02d%02d.pdf", year, month, day))
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/reports/weekly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xuất báo cáo PDF doanh thu hàng tuần",
            description = "Xuất file PDF báo cáo doanh thu theo tuần cụ thể. Nếu không truyền tham số, lấy tuần hiện tại."
    )
    public ResponseEntity<byte[]> exportWeeklyReport(
            @Parameter(description = "Năm (mặc định: năm hiện tại)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tuần (1-53, mặc định: tuần hiện tại)", example = "45")
            @RequestParam(required = false) Integer week) {

        // Validate and set defaults
        LocalDate today = LocalDate.now();
        year = year != null ? year : today.getYear();
        week = week != null ? week : today.get(java.time.temporal.WeekFields.ISO.weekOfYear());

        log.info("Admin exporting weekly revenue report for: year={}, week={}", year, week);

        // Generate report data
        RevenueReportResponse reportData = revenueService.generateWeeklyReport(year, week);

        // Generate PDF
        byte[] pdfBytes = pdfExportService.exportRevenuePdf(reportData);

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(String.format("revenue_report_weekly_%04d_W%02d.pdf", year, week))
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xuất báo cáo PDF doanh thu hàng tháng",
            description = "Xuất file PDF báo cáo doanh thu theo tháng cụ thể. Nếu không truyền tham số, lấy tháng hiện tại."
    )
    public ResponseEntity<byte[]> exportMonthlyReport(
            @Parameter(description = "Năm (mặc định: năm hiện tại)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Tháng (1-12, mặc định: tháng hiện tại)", example = "11")
            @RequestParam(required = false) Integer month) {

        // Validate and set defaults
        LocalDate today = LocalDate.now();
        year = year != null ? year : today.getYear();
        month = month != null ? month : today.getMonthValue();

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        log.info("Admin exporting monthly revenue report for: {}-{}", year, month);

        // Generate report data
        RevenueReportResponse reportData = revenueService.generateMonthlyReport(year, month);

        // Generate PDF
        byte[] pdfBytes = pdfExportService.exportRevenuePdf(reportData);

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(String.format("revenue_report_monthly_%04d%02d.pdf", year, month))
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/reports/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xuất báo cáo PDF doanh thu theo khoảng thời gian tùy chỉnh",
            description = "Xuất file PDF báo cáo doanh thu trong khoảng thời gian do admin tự chọn. " +
                    "Định dạng ngày: yyyy-MM-dd (ví dụ: 2025-11-01)"
    )
    public ResponseEntity<byte[]> exportCustomRangeReport(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", example = "2025-11-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", example = "2025-11-30", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Validate date range
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be after or equal to startDate");
        }

        log.info("Admin exporting custom range revenue report from {} to {}", startDate, endDate);

        // Generate report data
        RevenueReportResponse reportData = revenueService.generateCustomRangeReport(startDate, endDate);

        // Generate PDF
        byte[] pdfBytes = pdfExportService.exportRevenuePdf(reportData);

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(String.format("revenue_report_custom_%s_to_%s.pdf", startDate, endDate))
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}