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
@Tag(name = "System Usages", description = "Thống kê mức độ sử dụng của tất cả trạm sạc (Admin only)")
public class UsageController {

    StationUsageService stationUsageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy mức độ sử dụng tất cả trạm",
            description = "Trả về mức độ sử dụng của tất cả các trạm sạc theo ngày chỉ định. Mặc định lấy dữ liệu ngày hôm nay nếu không chỉ định ngày. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationUsageResponse>> listAll(
            @Parameter(description = "Ngày thống kê (yyyy-MM-dd)", example = "2025-11-04")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Admin fetching usage for all stations on date: {}", targetDate);

        return ApiResponse.<List<StationUsageResponse>>builder()
                .result(stationUsageService.getAllStationsUsageByDate(targetDate))
                .build();
    }
}

