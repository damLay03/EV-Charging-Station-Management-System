package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StaffChargingPointResponse;
import com.swp.evchargingstation.service.StaffDashboardService;
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
@RequestMapping("/api/my-stations/charging-points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Staff Charging Points", description = "Get charging points at staff's managed station")
public class StaffChargingPointController {

    StaffDashboardService staffDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Lấy danh sách trụ sạc tại trạm của tôi",
            description = "Trả về danh sách tất cả các trụ sạc tại trạm mà nhân viên quản lý, " +
                    "bao gồm thông tin trạng thái, giá cước, công suất, loại đầu nối, " +
                    "và thông tin phiên sạc hiện tại nếu có"
    )
    public ApiResponse<List<StaffChargingPointResponse>> getMyStationChargingPoints() {
        log.info("Staff requesting charging points at their managed station");
        return ApiResponse.<List<StaffChargingPointResponse>>builder()
                .result(staffDashboardService.getStaffChargingPoints())
                .build();
    }
}

