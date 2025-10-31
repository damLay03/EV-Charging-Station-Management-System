package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.service.DashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DashboardController {

    DashboardService dashboardService;

    /**
     * API 1: Lấy thống kê tổng quan (Cards trên cùng)
     * GET /api/dashboard/summary
     * @param period "today", "week", "month" (optional)
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false, defaultValue = "month") String period) {

        log.info("Driver requesting dashboard summary - period: {}", period);

        return ApiResponse.<DashboardSummaryResponse>builder()
                .result(dashboardService.getDashboardSummary(period))
                .build();
    }

    /**
     * API 2: Lấy thống kê theo giờ trong ngày (Biểu đồ)
     * GET /api/dashboard/hourly-sessions
     * @param date Ngày cần thống kê (optional, mặc định hôm nay)
     */
    @GetMapping("/hourly-sessions")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<HourlyChargingResponse>> getHourlyChargingSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Driver requesting hourly charging sessions - date: {}", date);

        return ApiResponse.<List<HourlyChargingResponse>>builder()
                .result(dashboardService.getHourlyChargingSessions(date))
                .build();
    }

    /**
     * API 3: Lấy danh sách trạm sạc yêu thích
     * GET /api/dashboard/favorite-stations
     * @param limit Số lượng trạm trả về (mặc định 5)
     */
    @GetMapping("/favorite-stations")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<FavoriteStationResponse>> getFavoriteStations(
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        log.info("Driver requesting favorite stations - limit: {}", limit);

        return ApiResponse.<List<FavoriteStationResponse>>builder()
                .result(dashboardService.getFavoriteStations(limit))
                .build();
    }

    /**
     * API 4: Lấy thống kê thói quen sạc
     * GET /api/dashboard/charging-statistics
     */
    @GetMapping("/charging-statistics")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingStatisticsResponse> getChargingStatistics() {

        log.info("Driver requesting charging statistics");

        return ApiResponse.<ChargingStatisticsResponse>builder()
                .result(dashboardService.getChargingStatistics())
                .build();
    }

    /**
     * API 5: Lấy thông tin gói plan hiện tại của driver
     * GET /api/dashboard/current-plan
     */
    @GetMapping("/current-plan")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<PlanResponse> getCurrentPlan() {

        log.info("Driver requesting current plan information");

        return ApiResponse.<PlanResponse>builder()
                .result(dashboardService.getCurrentPlan())
                .build();
    }
}
