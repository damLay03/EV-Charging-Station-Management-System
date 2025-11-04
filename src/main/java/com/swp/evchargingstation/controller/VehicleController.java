package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "My Vehicles", description = "API quản lý xe cá nhân của driver")
public class VehicleController {
    VehicleService vehicleService;


    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Tạo xe mới",
            description = "Driver tạo một xe mới với thông tin hãng xe, mẫu xe, biển số xe và năm sản xuất"
    )
    public ApiResponse<VehicleResponse> createVehicle(@RequestBody @Valid VehicleCreationRequest request) {
        log.info("Driver creating vehicle with license plate: {}", request.getLicensePlate());
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.createVehicle(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy danh sách xe của driver",
            description = "Trả về danh sách tất cả các xe mà driver đang sở hữu, bao gồm thông tin hãng xe, mẫu xe, biển số xe"
    )
    public ApiResponse<List<VehicleResponse>> getMyVehicles() {
        log.info("Driver fetching their vehicles");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getMyVehicles())
                .build();
    }

    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy chi tiết xe của driver",
            description = "Trả về thông tin chi tiết của một xe cụ thể mà driver sở hữu, bao gồm tất cả thông tin hãng, mẫu, biển số và năm sản xuất"
    )
    public ApiResponse<VehicleResponse> getVehicleById(@PathVariable String vehicleId) {
        log.info("Driver fetching vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.getMyVehicle(vehicleId))
                .build();
    }

    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Cập nhật thông tin xe",
            description = "Driver cập nhật thông tin xe của chính mình bao gồm hãng xe, mẫu xe, biển số xe, và năm sản xuất. Driver chỉ có thể cập nhật xe của mình"
    )
    public ApiResponse<VehicleResponse> updateVehicle(@PathVariable String vehicleId,
                                               @RequestBody @Valid VehicleUpdateRequest request) {
        log.info("Driver updating vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.updateMyVehicle(vehicleId, request))
                .build();
    }

    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Xóa xe",
            description = "Driver xóa một xe của chính mình khỏi hệ thống. Driver chỉ có thể xóa xe của mình"
    )
    public ApiResponse<Void> deleteVehicle(@PathVariable String vehicleId) {
        log.info("Driver deleting vehicle: {}", vehicleId);
        vehicleService.deleteMyVehicle(vehicleId);
        return ApiResponse.<Void>builder()
                .message("Vehicle deleted successfully")
                .build();
    }
}
