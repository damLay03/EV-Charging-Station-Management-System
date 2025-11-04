package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.service.ChargingSessionService;
import com.swp.evchargingstation.service.DashboardService;
import com.swp.evchargingstation.service.OverviewService;
import com.swp.evchargingstation.service.StaffDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Dashboard Management", description = "RESTful API Dashboard, Analytics & System Overview - Phân quyền theo role")
public class DashboardController {

    DashboardService dashboardService;
    ChargingSessionService chargingSessionService;
    StaffDashboardService staffDashboardService;
    OverviewService overviewService;

    // ==================== DRIVER ENDPOINTS ====================

    @GetMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy dashboard của driver",
            description = "Trả về thông tin dashboard của driver theo view được chỉ định. " +
                    "view=overview (mặc định): thông tin tổng quan đầy đủ, " +
                    "view=summary: thống kê tổng quan theo period (today/week/month)"
    )
    public ApiResponse<?> getDriverDashboard(
            @Parameter(description = "Loại view (overview, summary)", example = "overview")
            @RequestParam(defaultValue = "overview") String view,
            @Parameter(description = "Khoảng thời gian (today, week, month) - chỉ dùng với view=summary", example = "month")
            @RequestParam(required = false, defaultValue = "month") String period) {

        log.info("Driver requesting dashboard - view: {}, period: {}", view, period);

        return switch (view.toLowerCase()) {
            case "summary" -> ApiResponse.<DashboardSummaryResponse>builder()
                    .result(dashboardService.getDashboardSummary(period))
                    .build();
            case "overview" -> ApiResponse.<DriverDashboardResponse>builder()
                    .result(chargingSessionService.getMyDashboard())
                    .build();
            default -> throw new IllegalArgumentException("Invalid view: " + view);
        };
    }

    @GetMapping("/hourly-sessions")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy thống kê sạc theo giờ trong ngày",
            description = "Trả về dữ liệu sạc theo từng giờ trong ngày để hiển thị trên biểu đồ. Mặc định lấy dữ liệu hôm nay nếu không chỉ định ngày"
    )
    public ApiResponse<List<HourlyChargingResponse>> getHourlyChargingSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Driver requesting hourly charging sessions - date: {}", date);

        return ApiResponse.<List<HourlyChargingResponse>>builder()
                .result(dashboardService.getHourlyChargingSessions(date))
                .build();
    }

    @GetMapping("/favorite-stations")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy danh sách trạm sạc yêu thích",
            description = "Trả về danh sách các trạm sạc mà driver thường xuyên sử dụng nhất, có thể giới hạn số lượng trạm trả về"
    )
    public ApiResponse<List<FavoriteStationResponse>> getFavoriteStations(
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        log.info("Driver requesting favorite stations - limit: {}", limit);

        return ApiResponse.<List<FavoriteStationResponse>>builder()
                .result(dashboardService.getFavoriteStations(limit))
                .build();
    }

    @GetMapping("/charging-statistics")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy thống kê thói quen sạc",
            description = "Trả về thống kê về thói quen sạc của driver bao gồm giờ cao điểm, trạm yêu thích, mức tiêu thụ trung bình"
    )
    public ApiResponse<ChargingStatisticsResponse> getChargingStatistics() {

        log.info("Driver requesting charging statistics");

        return ApiResponse.<ChargingStatisticsResponse>builder()
                .result(dashboardService.getChargingStatistics())
                .build();
    }

    // ==================== STAFF ENDPOINTS ====================

    @GetMapping("/staff")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Lấy dashboard overview của nhân viên",
            description = "Trả về thông tin tổng quát của nhân viên bao gồm số phiên sạc hôm nay, doanh thu, trạng thái các trụ sạc tại trạm"
    )
    public ApiResponse<StaffDashboardResponse> getStaffDashboard() {
        log.info("Staff requesting dashboard overview");
        return ApiResponse.<StaffDashboardResponse>builder()
                .result(staffDashboardService.getStaffDashboard())
                .build();
    }

    // ==================== ADMIN - SYSTEM OVERVIEW ====================

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy dữ liệu tổng quan hệ thống",
            description = "Trả về thống kê tổng quan của hệ thống bao gồm tổng số trạm sạc, điểm sạc đang hoạt động, tổng số người dùng (driver), và doanh thu tháng hiện tại"
    )
    public ApiResponse<SystemOverviewResponse> getSystemOverview() {
        log.info("Admin requesting system overview");

        return ApiResponse.<SystemOverviewResponse>builder()
                .result(overviewService.getSystemOverview())
                .build();
    }
}


