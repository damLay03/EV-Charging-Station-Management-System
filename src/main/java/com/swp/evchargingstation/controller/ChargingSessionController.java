package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ChargingSessionResponse;
import com.swp.evchargingstation.dto.response.DriverDashboardResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.service.ChargingSessionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging-sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChargingSessionController {

    ChargingSessionService chargingSessionService;

    /**
     * Lấy dashboard overview của driver đang đăng nhập
     * Bao gồm: Tổng chi phí, tổng năng lượng, số phiên sạc, TB/tháng,
     * thông tin xe và % pin hiện tại
     *
     * Endpoint: GET /api/charging-sessions/my-dashboard
     */
    @GetMapping("/my-dashboard")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DriverDashboardResponse> getMyDashboard() {
        log.info("Driver requesting dashboard overview");
        return ApiResponse.<DriverDashboardResponse>builder()
                .result(chargingSessionService.getMyDashboard())
                .build();
    }

    /**
     * Lấy danh sách lịch sử phiên sạc của driver ��ang đăng nhập
     * Sắp xếp theo thời gian bắt đầu giảm dần (mới nhất trước)
     *
     * Endpoint: GET /api/charging-sessions/my-sessions
     */
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<ChargingSessionResponse>> getMySessions() {
        log.info("Driver requesting charging sessions history");
        return ApiResponse.<List<ChargingSessionResponse>>builder()
                .result(chargingSessionService.getMySessions())
                .build();
    }

    /**
     * Lấy chi tiết một phiên sạc theo sessionId
     * Driver chỉ có thể xem phiên sạc của mình
     *
     * Endpoint: GET /api/charging-sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingSessionResponse> getSessionById(@PathVariable String sessionId) {
        log.info("Driver requesting session detail: {}", sessionId);
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.getSessionById(sessionId))
                .build();
    }

    /**
     * Lấy thống kê phân tích theo tháng cho driver (5 tháng gần nhất)
     * Phục vụ cho tab "Phân tích" với 3 biểu đồ:
     * - Chi phí theo tháng (biểu đồ cột)
     * - Năng lượng tiêu thụ (biểu đồ đường)
     * - Số phiên sạc (biểu đồ cột)
     *
     * Endpoint: GET /api/charging-sessions/my-analytics/monthly
     */
    @GetMapping("/my-analytics/monthly")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<MonthlyAnalyticsResponse>> getMyMonthlyAnalytics() {
        log.info("Driver requesting monthly analytics");
        return ApiResponse.<List<MonthlyAnalyticsResponse>>builder()
                .result(chargingSessionService.getMyMonthlyAnalytics())
                .build();
    }

    // Phase 1: Start Charging
    @PostMapping("/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingSessionResponse> startCharging(@RequestBody @Valid StartChargingRequest request,
                                                              @AuthenticationPrincipal Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.startSession(request, driverId))
                .build();
    }

    // Phase 3: Stop Charging by user
    @PostMapping("/{sessionId}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingSessionResponse> stopCharging(@PathVariable String sessionId,
                                                             @AuthenticationPrincipal Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.stopSessionByUser(sessionId, driverId))
                .build();
    }
}
