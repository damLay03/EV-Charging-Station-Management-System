package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StationRevenueResponse;
import com.swp.evchargingstation.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Revenue", description = "API thống kê doanh thu theo ngày, tuần, tháng, năm dành cho quản trị viên")
public class RevenueController {

    RevenueService revenueService;

    /**
     * Lấy thống kê doanh thu theo ngày của từng trạm sạc
     * @param year Năm cần thống kê (mặc định: năm hiện tại)
     * @param month Tháng cần thống kê (mặc định: tháng hiện tại)
     * @param day Ngày cần thống kê (mặc định: ngày hiện tại)
     * @return Danh sách doanh thu của các trạm trong ngày
     */
    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thống kê doanh thu theo ngày",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo ngày. Mặc định lấy dữ liệu ngày hiện tại nếu không chỉ định. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationRevenueResponse>> getDailyRevenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day) {

        log.info("Admin requesting daily revenue - year: {}, month: {}, day: {}", year, month, day);

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(revenueService.getDailyRevenue(year, month, day))
                .build();
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thống kê doanh thu theo tuần",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo tuần. Mặc định lấy dữ liệu tuần hiện tại nếu không chỉ định. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationRevenueResponse>> getWeeklyRevenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer week
    ) {
        log.info("Admin requesting weekly revenue - year: {}, week: {}", year, week);

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(revenueService.getWeeklyRevenue(year, week))
                .build();
    }
    /**
     * Lấy thống kê doanh thu theo tháng của từng trạm sạc
     * @param year Năm cần thống kê (mặc định: năm hiện tại)
     * @param month Tháng cần thống kê (mặc định: tháng hiện tại)
     * @return Danh sách doanh thu của các trạm trong tháng
     */
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thống kê doanh thu theo tháng",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo tháng. Mặc định lấy dữ liệu tháng hiện tại nếu không chỉ định. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationRevenueResponse>> getMonthlyRevenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        log.info("Admin requesting monthly revenue - year: {}, month: {}", year, month);

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(revenueService.getMonthlyRevenue(year, month))
                .build();
    }

    /**
     * Lấy thống kê doanh thu theo năm của từng trạm sạc (tất cả các tháng)
     * @param year Năm cần thống kê (mặc định: năm hiện tại)
     * @return Danh sách doanh thu của các trạm trong cả năm
     */
    @GetMapping("/yearly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thống kê doanh thu theo năm",
            description = "Trả về thống kê doanh thu của từng trạm sạc theo năm (tất cả các tháng). Mặc định lấy dữ liệu năm hiện tại nếu không chỉ định. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StationRevenueResponse>> getYearlyRevenue(
            @RequestParam(required = false) Integer year) {

        log.info("Admin requesting yearly revenue - year: {}", year);

        return ApiResponse.<List<StationRevenueResponse>>builder()
                .result(revenueService.getYearlyRevenue(year))
                .build();
    }
}