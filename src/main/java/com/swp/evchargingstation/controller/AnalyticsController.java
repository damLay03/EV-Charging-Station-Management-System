package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.service.ChargingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Analytics Management", description = "RESTful API phân tích dữ liệu sạc - Driver only")
public class AnalyticsController {

    ChargingSessionService chargingSessionService;

    // ==================== DRIVER - ANALYTICS ====================

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy dữ liệu phân tích của tôi",
            description = "Trả về dữ liệu phân tích theo loại được chỉ định. " +
                    "period=monthly (mặc định): 5 tháng gần nhất với chi phí, năng lượng tiêu thụ, số phiên sạc"
    )
    public ApiResponse<List<MonthlyAnalyticsResponse>> getAnalytics(
            @Parameter(description = "Loại phân tích (monthly)", example = "monthly")
            @RequestParam(defaultValue = "monthly") String period) {

        log.info("Driver requesting analytics - period: {}", period);

        if ("monthly".equalsIgnoreCase(period)) {
            return ApiResponse.<List<MonthlyAnalyticsResponse>>builder()
                    .result(chargingSessionService.getMyMonthlyAnalytics())
                    .build();
        } else {
            throw new IllegalArgumentException("Invalid analytics period: " + period + ". Supported: monthly");
        }
    }
}

