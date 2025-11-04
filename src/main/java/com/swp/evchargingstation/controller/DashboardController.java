package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.service.ChargingSessionService;
import com.swp.evchargingstation.service.DashboardService;
import com.swp.evchargingstation.service.StaffDashboardService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/my-dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Driver Dashboard", description = "Dashboard và thống kê sạc của driver hiện tại")
public class DashboardController {

    DashboardService dashboardService;
    ChargingSessionService chargingSessionService;

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy dashboard overview của driver",
            description = "Trả về thông tin tổng quát của driver bao gồm tổng chi phí, tổng năng lượng, số phiên sạc, trung bình/tháng, thông tin xe và phần trăm pin hiện tại"
    )
    public ApiResponse<DriverDashboardResponse> getMyDashboard() {
        log.info("Driver requesting dashboard overview");
        return ApiResponse.<DriverDashboardResponse>builder()
                .result(chargingSessionService.getMyDashboard())
                .build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy thống kê tổng quan dashboard",
            description = "Trả về thống kê tổng quan bao gồm tổng chi phí, tổng năng lượng, số phiên sạc theo khoảng thời gian (hôm nay, tuần, tháng)"
    )
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false, defaultValue = "month") String period) {

        log.info("Driver requesting dashboard summary - period: {}", period);

        return ApiResponse.<DashboardSummaryResponse>builder()
                .result(dashboardService.getDashboardSummary(period))
                .build();
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
            summary = "Lấy danh sách trạm sạc yêu thích",
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
            summary = "Lấy thống kê thói quen sạc",
            description = "Trả về thống kê về thói quen sạc của driver bao gồm giờ cao điểm, trạm yêu thích, mức tiêu thụ trung bình"
    )
    public ApiResponse<ChargingStatisticsResponse> getChargingStatistics() {

        log.info("Driver requesting charging statistics");

        return ApiResponse.<ChargingStatisticsResponse>builder()
                .result(dashboardService.getChargingStatistics())
                .build();
    }
}


