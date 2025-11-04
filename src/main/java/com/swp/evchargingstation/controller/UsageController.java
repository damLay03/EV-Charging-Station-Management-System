package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StationUsageResponse;
import com.swp.evchargingstation.service.StationUsageService;
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
@RequestMapping("/api/usages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Usage Management", description = "RESTful API thống kê mức độ sử dụng trạm sạc - Admin/Staff")
public class UsageController {

    StationUsageService stationUsageService;

    // ==================== ADMIN - ALL STATIONS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy mức độ sử dụng tất cả trạm",
            description = "Trả về mức độ sử dụng của tất cả các trạm sạc theo ngày chỉ định. Mặc định lấy dữ liệu ngày hôm nay nếu không chỉ định ngày"
    )
    public ApiResponse<List<StationUsageResponse>> getAllStationsUsage(
            @Parameter(description = "Ngày thống kê (yyyy-MM-dd)", example = "2025-11-04")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Admin fetching usage for all stations on date: {}", targetDate);

        return ApiResponse.<List<StationUsageResponse>>builder()
                .result(stationUsageService.getAllStationsUsageByDate(targetDate))
                .build();
    }

    // ==================== ADMIN/STAFF - SPECIFIC STATION ====================

    @GetMapping("/stations/{stationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "[ADMIN/STAFF] Lấy mức độ sử dụng trạm cụ thể",
            description = "Trả về mức độ sử dụng của một trạm sạc theo ngày chỉ định. Mặc định lấy dữ liệu ngày hôm nay nếu không chỉ định ngày bao gồm số phiên sạc, tỷ lệ sử dụng, năng lượng tiêu thụ"
    )
    public ApiResponse<StationUsageResponse> getStationUsage(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId,
            @Parameter(description = "Ngày thống kê (yyyy-MM-dd)", example = "2025-11-04")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Fetching usage for station: {} on date: {}", stationId, targetDate);

        return ApiResponse.<StationUsageResponse>builder()
                .result(stationUsageService.getStationUsageByDate(stationId, targetDate))
                .build();
    }
}

