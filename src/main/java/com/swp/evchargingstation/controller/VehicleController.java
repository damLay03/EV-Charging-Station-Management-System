package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.service.VehicleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VehicleController {
    VehicleService vehicleService;

    // =====================================================DRIVER===========================================================

    // NOTE: Driver tạo xe mới
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<VehicleResponse> createVehicle(@RequestBody @Valid VehicleCreationRequest request) {
        log.info("Driver creating vehicle with license plate: {}", request.getLicensePlate());
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.createVehicle(request))
                .build();
    }

    // NOTE: Driver lấy danh sách xe của mình
    @GetMapping("/my-vehicles")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<VehicleResponse>> getMyVehicles() {
        log.info("Driver fetching their vehicles");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getMyVehicles())
                .build();
    }

    // NOTE: Driver lấy chi tiết một xe của mình
    @GetMapping("/my-vehicles/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<VehicleResponse> getMyVehicle(@PathVariable String vehicleId) {
        log.info("Driver fetching vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.getMyVehicle(vehicleId))
                .build();
    }

    // NOTE: Driver cập nhật xe của mình
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<VehicleResponse> updateMyVehicle(@PathVariable String vehicleId,
                                                        @RequestBody @Valid VehicleUpdateRequest request) {
        log.info("Driver updating vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.updateMyVehicle(vehicleId, request))
                .build();
    }

    // NOTE: Driver xóa xe của mình
    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<Void> deleteMyVehicle(@PathVariable String vehicleId) {
        log.info("Driver deleting vehicle: {}", vehicleId);
        vehicleService.deleteMyVehicle(vehicleId);
        return ApiResponse.<Void>builder()
                .message("Vehicle deleted successfully")
                .build();
    }

    // =====================================================ADMIN===========================================================

    // NOTE: Admin lấy danh sách xe của một driver
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        log.info("Admin fetching vehicles of driver: {}", driverId);
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getVehiclesByDriver(driverId))
                .build();
    }
}

