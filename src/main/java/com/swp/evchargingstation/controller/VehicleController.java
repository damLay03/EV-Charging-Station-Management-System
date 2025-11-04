package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Vehicles Management", description = "RESTful API quản lý xe, hãng xe, mẫu xe - Phân quyền theo role")
public class VehicleController {
    VehicleService vehicleService;

    // ==================== PUBLIC - BRANDS & MODELS ====================

    @GetMapping("/brands")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách tất cả hãng xe",
            description = "Trả về danh sách tất cả các hãng xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleBrandResponse>> getAllBrands() {
        log.info("Fetching all vehicle brands");
        return ApiResponse.<List<VehicleBrandResponse>>builder()
                .result(vehicleService.getAllBrands())
                .build();
    }

    @GetMapping("/brands/{brand}/models")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách mẫu xe theo hãng",
            description = "Trả về danh sách tất cả các mẫu xe của một hãng cụ thể"
    )
    public ApiResponse<List<VehicleModelResponse>> getModelsByBrand(
            @Parameter(description = "Tên hãng xe", example = "TESLA")
            @PathVariable VehicleBrand brand) {
        log.info("Fetching models for brand: {}", brand);
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getModelsByBrand(brand))
                .build();
    }

    @GetMapping("/models")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách tất cả mẫu xe",
            description = "Trả về danh sách tất cả các mẫu xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleModelResponse>> getAllModels() {
        log.info("Fetching all vehicle models");
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getAllModels())
                .build();
    }

    // ==================== STAFF - VEHICLE LOOKUP ====================

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Tìm kiếm xe theo biển số",
            description = "Staff nhập biển số xe để lấy tất cả thông tin xe và chủ xe cần thiết để bắt đầu phiên sạc"
    )
    public ApiResponse<VehicleLookupResponse> lookupByLicensePlate(
            @Parameter(description = "Biển số xe", example = "30A-12345")
            @RequestParam String licensePlate) {
        log.info("Staff searching vehicle with license plate: {}", licensePlate);
        return ApiResponse.<VehicleLookupResponse>builder()
                .result(vehicleService.lookupVehicleByLicensePlate(licensePlate))
                .build();
    }

    // ==================== DRIVER ENDPOINTS ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Tạo xe mới",
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Xóa xe",
            description = "Driver xóa một xe của chính mình khỏi hệ thống. Driver chỉ có thể xóa xe của mình"
    )
    public ApiResponse<Void> deleteVehicle(@PathVariable String vehicleId) {
        log.info("Driver deleting vehicle: {}", vehicleId);
        vehicleService.deleteMyVehicle(vehicleId);
        return ApiResponse.<Void>builder()
                .message("Vehicle deleted successfully")
                .build();
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy danh sách xe của driver",
            description = "Admin lấy danh sách tất cả các xe của một driver cụ thể"
    )
    public ApiResponse<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        log.info("Admin fetching vehicles of driver: {}", driverId);
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getVehiclesByDriver(driverId))
                .build();
    }
}
