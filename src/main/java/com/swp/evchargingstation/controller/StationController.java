package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.response.StationOverviewResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.enums.StationStatus;
import com.swp.evchargingstation.service.StationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationController {
    StationService stationService;

    // Endpoint overview: trả về tất cả trạm + cờ active cho FE (nhẹ hơn so với full StationResponse)
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationOverviewResponse>> getOverview() {
        log.info("Admin fetching station overview");
        return ApiResponse.<List<StationOverviewResponse>>builder()
                .result(stationService.getAllOverview())
                .build();
    }

    // Danh sách trạm: ưu tiên lọc theo active nếu truyền, nếu không thì lọc theo status
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationResponse>> getStations(
            @RequestParam(value = "status", required = false) StationStatus status,
            @RequestParam(value = "active", required = false) Boolean active) {
        log.info("Admin fetching stations - status: {}, active: {}", status, active);
        List<StationResponse> result = (active != null)
                ? stationService.getStationsByActive(active)
                : stationService.getStations(status);
        return ApiResponse.<List<StationResponse>>builder()
                .result(result)
                .build();
    }

    // Cập nhật trạng thái cụ thể (truyền enum trực tiếp)
    @PatchMapping("/{stationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> updateStationStatus(@PathVariable String stationId, @RequestParam StationStatus status) {
        log.info("Admin updating station {} to status {}", stationId, status);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStationStatus(stationId, status))
                .build();
    }

    // Đặt trạng thái hoạt động (OPERATIONAL)
    @PatchMapping("/{stationId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> activate(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.activate(stationId))
                .build();
    }

    // Đặt trạng thái OUT_OF_SERVICE (ngưng hoạt động)
    @PatchMapping("/{stationId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> deactivate(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.deactivate(stationId))
                .build();
    }

    // Toggle giữa OPERATIONAL và OUT_OF_SERVICE
    @PatchMapping("/{stationId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> toggle(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.toggle(stationId))
                .build();
    }
}
