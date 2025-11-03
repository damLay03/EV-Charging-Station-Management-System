package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.dto.response.VehicleBrandResponse;
import com.swp.evchargingstation.dto.response.VehicleModelResponse;
import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.enums.VehicleBrand;
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
@Tag(name = "Vehicles", description = "API quản lý xe của driver (hãng xe, mẫu xe, danh sách xe)")
public class VehicleController {
    VehicleService vehicleService;

    // =====================================================PUBLIC===========================================================

    // NOTE: Lấy danh sách tất cả các hãng xe (không cần đăng nhập)
    @GetMapping("/brands")
    @Operation(
            summary = "Lấy danh sách tất cả hãng xe",
            description = "Trả về danh sách tất cả các hãng xe có sẵn trong hệ thống. Không cần đăng nhập để truy cập"
    )
    public ApiResponse<List<VehicleBrandResponse>> getAllBrands() {
        log.info("Fetching all vehicle brands");
        return ApiResponse.<List<VehicleBrandResponse>>builder()
                .result(vehicleService.getAllBrands())
                .build();
    }

    // NOTE: Lấy danh sách models theo brand (không cần đăng nhập)
    @GetMapping("/brands/{brand}/models")
    @Operation(
            summary = "Lấy danh sách mẫu xe theo hãng",
            description = "Trả về danh sách tất cả các mẫu xe của một hãng cụ thể. Không cần đăng nhập để truy cập"
    )
    public ApiResponse<List<VehicleModelResponse>> getModelsByBrand(@PathVariable VehicleBrand brand) {
        log.info("Fetching models for brand: {}", brand);
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getModelsByBrand(brand))
                .build();
    }

    // NOTE: Lấy danh sách tất cả models (không cần đăng nhập)
    @GetMapping("/models")
    @Operation(
            summary = "Lấy danh sách tất cả mẫu xe",
            description = "Trả về danh sách tất cả các mẫu xe có sẵn trong hệ thống. Không cần đăng nhập để truy cập"
    )
    public ApiResponse<List<VehicleModelResponse>> getAllModels() {
        log.info("Fetching all vehicle models");
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getAllModels())
                .build();
    }

    // =====================================================DRIVER===========================================================

    // NOTE: Driver tạo xe mới
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

    // NOTE: Driver lấy danh sách xe của mình
    @GetMapping("/my-vehicles")
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

    // NOTE: Driver lấy chi tiết một xe của mình
    @GetMapping("/my-vehicles/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy chi tiết xe của driver",
            description = "Trả về thông tin chi tiết của một xe cụ thể mà driver sở hữu, bao gồm tất cả thông tin hãng, mẫu, biển số và năm sản xuất"
    )
    public ApiResponse<VehicleResponse> getMyVehicle(@PathVariable String vehicleId) {
        log.info("Driver fetching vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.getMyVehicle(vehicleId))
                .build();
    }

    // NOTE: Driver cập nhật xe của mình
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Cập nhật thông tin xe",
            description = "Driver cập nhật thông tin xe của chính mình bao gồm hãng xe, mẫu xe, biển số xe, và năm sản xuất. Driver chỉ có thể cập nhật xe của mình"
    )
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
    @Operation(
            summary = "Xóa xe",
            description = "Driver xóa một xe của chính mình khỏi hệ thống. Driver chỉ có thể xóa xe của mình"
    )
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
    @Operation(
            summary = "Lấy danh sách xe của driver (ADMIN only)",
            description = "ADMIN lấy danh sách tất cả các xe của một driver cụ thể. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        log.info("Admin fetching vehicles of driver: {}", driverId);
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getVehiclesByDriver(driverId))
                .build();
    }
}
