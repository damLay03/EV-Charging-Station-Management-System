package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ChargingSessionResponse;
import com.swp.evchargingstation.dto.response.DriverDashboardResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.service.ChargingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Charging Sessions", description = "API quản lý phiên sạc của driver")
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

    /**
     * Lấy danh sách lịch sử phiên sạc của driver đang đăng nhập
     * Sắp xếp theo thời gian bắt đầu giảm dần (mới nhất trước)
     *
     * Endpoint: GET /api/charging-sessions/my-sessions
     */
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy danh sách lịch sử phiên sạc của driver",
            description = "Trả về danh sách tất cả các phiên sạc của driver đã đăng nhập, sắp xếp theo thời gian bắt đầu giảm dần (mới nhất trước)"
    )
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
    @Operation(
            summary = "Lấy chi tiết phiên sạc theo ID",
            description = "Trả về chi tiết của một phiên sạc cụ thể theo sessionId. Driver chỉ có thể xem phiên sạc của chính mình"
    )
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
    @Operation(
            summary = "Lấy thống kê phân tích theo tháng của driver",
            description = "Trả về dữ liệu phân tích 5 tháng gần nhất bao gồm chi phí, năng lượng tiêu thụ và số phiên sạc. Dùng để hiển thị các biểu đồ trên tab phân tích"
    )
    public ApiResponse<List<MonthlyAnalyticsResponse>> getMyMonthlyAnalytics() {
        log.info("Driver requesting monthly analytics");
        return ApiResponse.<List<MonthlyAnalyticsResponse>>builder()
                .result(chargingSessionService.getMyMonthlyAnalytics())
                .build();
    }

    //Start Charging
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Bắt đầu phiên sạc mới",
            description = "Tạo một phiên sạc mới cho driver với các thông tin về trạm sạc, điểm sạc và dữ liệu xe"
    )
    public ApiResponse<ChargingSessionResponse> startCharging(@RequestBody @Valid StartChargingRequest request,
                                                              @AuthenticationPrincipal Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.startSession(request, driverId))
                .build();
    }

    //Stop Charging by user
    @PostMapping("/{sessionId}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Dừng phiên sạc",
            description = "Dừng phiên sạc hiện tại của driver. Driver chỉ có thể dừng phiên sạc của chính mình"
    )
    public ApiResponse<ChargingSessionResponse> stopCharging(@PathVariable String sessionId,
                                                             @AuthenticationPrincipal Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.stopSessionByUser(sessionId, driverId))
                .build();
    }
}
