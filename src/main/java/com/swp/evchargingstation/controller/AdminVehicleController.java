package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Vehicle Management", description = "API quản lý xe của driver (dành cho admin)")
public class AdminVehicleController {
    VehicleService vehicleService;

    @GetMapping("/{driverId}/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách xe của driver",
            description = "Admin lấy danh sách tất cả các xe của một driver cụ thể. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        log.info("Admin fetching vehicles of driver: {}", driverId);
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getVehiclesByDriver(driverId))
                .build();
    }
}

