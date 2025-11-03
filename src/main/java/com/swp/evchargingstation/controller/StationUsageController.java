package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StationUsageResponse;
import com.swp.evchargingstation.service.StationUsageService;
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
@RequestMapping("/api/station-usage")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Station Usage", description = "API thống kê mức độ sử dụng trạm sạc theo ngày")
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
    @Operation(
            summary = "Lấy mức độ sử dụng trạm trong ngày hôm nay",
            description = "Trả về mức độ sử dụng của một trạm sạc trong ngày hôm nay bao gồm số phiên sạc, tỷ lệ sử dụng, năng lượng tiêu thụ"
    )
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
    @Operation(
            summary = "Lấy mức độ sử dụng trạm theo ngày cụ thể",
            description = "Trả về mức độ sử dụng của một trạm sạc theo ngày chỉ định. Mặc định lấy dữ liệu ngày hôm nay nếu không chỉ định ngày"
    )
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
    @Operation(
            summary = "Lấy mức độ sử dụng tất cả trạm trong ngày hôm nay",
            description = "Trả về mức độ sử dụng của tất cả các trạm sạc trong ngày hôm nay. Chỉ quản trị viên có quyền truy cập"
    )
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
    @Operation(
            summary = "Lấy mức độ sử dụng tất cả trạm theo ngày cụ thể",
            description = "Trả về mức độ sử dụng của tất cả các trạm sạc theo ngày chỉ định. Mặc định lấy dữ liệu ngày hôm nay nếu không chỉ định ngày. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationUsageResponse>> getAllStationsUsageByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Fetching usage for all stations on date: {}", targetDate);

        return ApiResponse.<List<StationUsageResponse>>builder()
                .result(stationUsageService.getAllStationsUsageByDate(targetDate))
                .build();
    }
}