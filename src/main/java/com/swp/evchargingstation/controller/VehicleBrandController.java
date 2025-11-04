package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.VehicleBrandResponse;
import com.swp.evchargingstation.dto.response.VehicleModelResponse;
import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-brands")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Vehicle Brands", description = "API quản lý hãng xe")
public class VehicleBrandController {
    VehicleService vehicleService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả hãng xe",
            description = "Trả về danh sách tất cả các hãng xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleBrandResponse>> getAllBrands() {
        log.info("Fetching all vehicle brands");
        return ApiResponse.<List<VehicleBrandResponse>>builder()
                .result(vehicleService.getAllBrands())
                .build();
    }

    @GetMapping("/{brand}/models")
    @Operation(
            summary = "Lấy danh sách mẫu xe theo hãng",
            description = "Trả về danh sách tất cả các mẫu xe của một hãng cụ thể"
    )
    public ApiResponse<List<VehicleModelResponse>> getModelsByBrand(@PathVariable VehicleBrand brand) {
        log.info("Fetching models for brand: {}", brand);
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getModelsByBrand(brand))
                .build();
    }
}

