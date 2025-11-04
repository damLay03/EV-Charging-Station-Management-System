package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.service.StationService;
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

import java.util.List;

@RestController
@RequestMapping("/api/staffs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Staff Management", description = "Quản lý nhân viên (Admin only)")
public class StaffController {

    StationService stationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách tất cả nhân viên",
            description = "Trả về danh sách tất cả nhân viên để chọn khi tạo hoặc cập nhật trạm sạc. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StaffSummaryResponse>> getAllStaff() {
        log.info("Admin fetching all staff for station assignment");
        return ApiResponse.<List<StaffSummaryResponse>>builder()
                .result(stationService.getAllStaff())
                .build();
    }
}

