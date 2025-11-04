package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.VehicleModelResponse;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-models")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Vehicle Models", description = "API quản lý mẫu xe")
public class VehicleModelController {
    VehicleService vehicleService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả mẫu xe",
            description = "Trả về danh sách tất cả các mẫu xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleModelResponse>> getAllModels() {
        log.info("Fetching all vehicle models");
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getAllModels())
                .build();
    }
}

