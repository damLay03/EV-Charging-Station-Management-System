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
@Tag(name = "Revenue Statistics", description = "Thống kê doanh thu của hệ thống - Admin only")
public class RevenueController {

    RevenueService revenueService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thống kê doanh thu",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo khoảng thời gian được chỉ định (ngày, tuần, tháng, năm). Chỉ quản trị viên có quyền truy cập"
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

        List<StationRevenueResponse> result = switch (period.toLowerCase()) {
            case "daily" -> revenueService.getDailyRevenue(year, month, day);
            case "weekly" -> revenueService.getWeeklyRevenue(year, week);
            case "monthly" -> revenueService.getMonthlyRevenue(year, month);
            case "yearly" -> revenueService.getYearlyRevenue(year);
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(result)
                .build();
    }
}