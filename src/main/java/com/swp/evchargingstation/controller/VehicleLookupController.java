package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.VehicleLookupResponse;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/vehicle-lookup")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Vehicle Lookup", description = "API tìm kiếm và xác minh thông tin xe")
public class VehicleLookupController {
    VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Tìm kiếm xe theo biển số",
            description = "Staff nhập biển số xe để lấy tất cả thông tin xe và chủ xe cần thiết để bắt đầu phiên sạc"
    )
    public ApiResponse<VehicleLookupResponse> lookupByLicensePlate(@RequestParam String licensePlate) {
        log.info("Staff searching vehicle with license plate: {}", licensePlate);
        return ApiResponse.<VehicleLookupResponse>builder()
                .result(vehicleService.lookupVehicleByLicensePlate(licensePlate))
                .build();
    }
}

