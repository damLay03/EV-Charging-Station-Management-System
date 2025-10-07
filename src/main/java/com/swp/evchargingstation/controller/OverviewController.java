package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.response.SystemOverviewResponse;
import com.swp.evchargingstation.service.OverviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OverviewController {

    OverviewService overviewService;

    /**
     * Lấy dữ liệu tổng quan hệ thống
     * Bao gồm: Tổng số trạm sạc, điểm sạc đang hoạt động,
     * tổng số người dùng (driver), doanh thu tháng hiện tại
     *
     * Chỉ ADMIN mới có quyền truy cập
     *
     * @return SystemOverviewResponse chứa các thông số tổng quan
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemOverviewResponse> getSystemOverview() {
        log.info("Admin requesting system overview");

        return ApiResponse.<SystemOverviewResponse>builder()
                .result(overviewService.getSystemOverview())
                .build();
    }

    /**
     * Lấy tổng số trạm sạc trong hệ thống
     *
     * Chỉ ADMIN mới có quyền truy cập
     *
     * @return Tổng số trạm sạc
     */
    @GetMapping("/total-stations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getTotalStations() {
        log.info("Admin requesting total stations count");

        return ApiResponse.<Long>builder()
                .result(overviewService.getTotalStations())
                .build();
    }

    /**
     * Lấy tổng số driver trong hệ thống
     *
     * Chỉ ADMIN mới có quyền truy cập
     *
     * @return Tổng số driver
     */
    @GetMapping("/total-drivers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getTotalDrivers() {
        log.info("Admin requesting total drivers count");

        return ApiResponse.<Long>builder()
                .result(overviewService.getTotalDrivers())
                .build();
    }
}