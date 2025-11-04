package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-plans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Driver Plans", description = "Gói cước của driver hiện tại")
public class PlanController {

    DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy gói plan hiện tại của driver",
            description = "Trả về thông tin chi tiết về gói plan mà driver hiện đang sử dụng, bao gồm giá, lợi ích, thời hạn"
    )
    public ApiResponse<PlanResponse> getCurrentPlan() {
        log.info("Driver requesting current plan information");
        return ApiResponse.<PlanResponse>builder()
                .result(dashboardService.getCurrentPlan())
                .build();
    }
}

