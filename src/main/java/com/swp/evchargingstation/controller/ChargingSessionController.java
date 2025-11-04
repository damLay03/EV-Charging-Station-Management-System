package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.dto.request.StartChargingRequest;
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
@RequestMapping("/api/my-sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Driver Charging Sessions", description = "API quản lý phiên sạc của driver hiện tại")
public class ChargingSessionController {

    ChargingSessionService chargingSessionService;

    @GetMapping
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

    @PostMapping("/{sessionId}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Dừng phiên sạc",
            description = "Dừng phiên sạc hiện tại của driver. Driver chỉ có thể dừng phiên sạc của chính mình"
    )
    public ApiResponse<ChargingSessionResponse> stopCharging(@PathVariable String sessionId,
                                                             @AuthenticationPrincipal Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        log.info("Driver {} stopping charging session: {}", driverId, sessionId);
        return ApiResponse.<ChargingSessionResponse>builder()
                .result(chargingSessionService.stopSessionByUser(sessionId, driverId))
                .build();
    }
}
