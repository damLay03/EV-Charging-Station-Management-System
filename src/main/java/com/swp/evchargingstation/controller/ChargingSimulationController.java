package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.ChargingProgressResponse;
import com.swp.evchargingstation.dto.response.ChargingSimulationResponse;
import com.swp.evchargingstation.service.ChargingSimulationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/charging-simulation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChargingSimulationController {

    ChargingSimulationService chargingSimulationService;

    /**
     * Bắt đầu phiên sạc mới
     * Driver chọn xe, trạm sạc và % pin mục tiêu
     *
     * Endpoint: POST /api/charging-simulation/start
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingProgressResponse> startCharging(@RequestBody @Valid StartChargingRequest request) {
        log.info("Starting charging simulation for vehicle {}", request.getVehicleId());
        return ApiResponse.<ChargingProgressResponse>builder()
                .result(chargingSimulationService.startCharging(request))
                .message("Charging session started successfully")
                .build();
    }

    /**
     * Giả lập tiến trình sạc (tăng % pin theo thời gian)
     * Có thể gọi endpoint này định kỳ từ frontend để cập nhật UI
     * hoặc backend có thể tự động chạy background job
     *
     * Endpoint: POST /api/charging-simulation/{sessionId}/progress
     */
    @PostMapping("/{sessionId}/progress")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingSimulationResponse> simulateProgress(@PathVariable String sessionId) {
        log.info("Simulating charging progress for session {}", sessionId);
        return ApiResponse.<ChargingSimulationResponse>builder()
                .result(chargingSimulationService.simulateChargingProgress(sessionId))
                .build();
    }

    /**
     * Lấy thông tin tiến trình sạc hiện tại
     * Hiển thị % pin, năng lượng đã nạp, chi phí, thời gian còn lại, v.v.
     *
     * Endpoint: GET /api/charging-simulation/{sessionId}
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingProgressResponse> getProgress(@PathVariable String sessionId) {
        log.info("Getting charging progress for session {}", sessionId);
        return ApiResponse.<ChargingProgressResponse>builder()
                .result(chargingSimulationService.getChargingProgress(sessionId))
                .build();
    }

    /**
     * Dừng phiên sạc trước khi đạt mục tiêu
     * Driver có thể chủ động dừng sạc bất kỳ lúc nào
     *
     * Endpoint: POST /api/charging-simulation/{sessionId}/stop
     */
    @PostMapping("/{sessionId}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ChargingProgressResponse> stopCharging(@PathVariable String sessionId) {
        log.info("Stopping charging session {}", sessionId);
        return ApiResponse.<ChargingProgressResponse>builder()
                .result(chargingSimulationService.stopCharging(sessionId))
                .message("Charging session stopped successfully")
                .build();
    }
}

