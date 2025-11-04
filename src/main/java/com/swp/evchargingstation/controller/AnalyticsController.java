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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Driver Analytics", description = "Phân tích dữ liệu sạc của driver hiện tại")
public class AnalyticsController {

    ChargingSessionService chargingSessionService;

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy dữ liệu phân tích",
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
            throw new IllegalArgumentException("Invalid analytics period: " + period);
        }
    }
}

