package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.response.StationUsageResponse;
import com.swp.evchargingstation.service.StationUsageService;
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
@RequestMapping("/api/station-usage")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationUsageController {

    StationUsageService stationUsageService;

    /**
     * Lấy mức độ sử dụng của MỘT trạm trong ngày hôm nay
     *
     * GET /api/station-usage/{stationId}/today
     *
     * @param stationId ID của trạm cần xem
     * @return StationUsageResponse với thông tin chi tiết usage
     */
    @GetMapping("/{stationId}/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<StationUsageResponse> getStationUsageToday(@PathVariable String stationId) {
        log.info("Fetching today's usage for station: {}", stationId);

        return ApiResponse.<StationUsageResponse>builder()
                .result(stationUsageService.getStationUsageToday(stationId))
                .build();
    }

    /**
     * Lấy mức độ sử dụng của MỘT trạm theo ngày cụ thể
     *
     * GET /api/station-usage/{stationId}?date=2025-10-04
     *
     * @param stationId ID của trạm
     * @param date Ngày cần xem (format: yyyy-MM-dd), mặc định là hôm nay
     * @return StationUsageResponse
     */
    @GetMapping("/{stationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<StationUsageResponse> getStationUsageByDate(
            @PathVariable String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Fetching usage for station: {} on date: {}", stationId, targetDate);

        return ApiResponse.<StationUsageResponse>builder()
                .result(stationUsageService.getStationUsageByDate(stationId, targetDate))
                .build();
    }

    /**
     * Lấy mức độ sử dụng của TẤT CẢ trạm trong ngày hôm nay
     *
     * GET /api/station-usage/all/today
     *
     * @return Danh sách StationUsageResponse của tất cả trạm
     */
    @GetMapping("/all/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationUsageResponse>> getAllStationsUsageToday() {
        log.info("Fetching today's usage for all stations");

        return ApiResponse.<List<StationUsageResponse>>builder()
                .result(stationUsageService.getAllStationsUsageToday())
                .build();
    }

    /**
     * Lấy mức độ sử dụng của TẤT CẢ trạm theo ngày cụ thể
     *
     * GET /api/station-usage/all?date=2025-10-04
     *
     * @param date Ngày cần xem, mặc định là hôm nay
     * @return Danh sách StationUsageResponse
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationUsageResponse>> getAllStationsUsageByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Fetching usage for all stations on date: {}", targetDate);

        return ApiResponse.<List<StationUsageResponse>>builder()
                .result(stationUsageService.getAllStationsUsageByDate(targetDate))
                .build();
    }
}