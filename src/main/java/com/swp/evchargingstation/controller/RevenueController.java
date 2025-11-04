package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StationRevenueResponse;
import com.swp.evchargingstation.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revenues")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Revenue Management", description = "RESTful API thống kê doanh thu - Admin only")
public class RevenueController {

    RevenueService revenueService;

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
}