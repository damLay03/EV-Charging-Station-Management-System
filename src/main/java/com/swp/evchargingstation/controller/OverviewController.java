package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.SystemOverviewResponse;
import com.swp.evchargingstation.service.OverviewService;
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
@RequestMapping("/api/overview")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Overview", description = "API tổng quan hệ thống dành cho quản trị viên")
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
    @Operation(
            summary = "Lấy dữ liệu tổng quan hệ thống",
            description = "Trả về thống kê tổng quan của hệ thống bao gồm tổng số trạm sạc, điểm sạc đang hoạt động, tổng số người dùng (driver), và doanh thu tháng hiện tại. Chỉ quản trị viên mới có quyền truy cập"
    )
    public ApiResponse<SystemOverviewResponse> getSystemOverview() {
        log.info("Admin requesting system overview");

        return ApiResponse.<SystemOverviewResponse>builder()
                .result(overviewService.getSystemOverview())
                .build();
    }
}