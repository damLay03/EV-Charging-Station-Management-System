package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ChargingPointCreationRequest;
import com.swp.evchargingstation.dto.request.ChargingPointUpdateRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.ChargingPointResponse;
import com.swp.evchargingstation.dto.response.StaffChargingPointResponse;
import com.swp.evchargingstation.service.StaffDashboardService;
import com.swp.evchargingstation.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations/{stationId}/charging-points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Charging Points", description = "Manage charging points at stations")
public class ChargingPointController {

    StaffDashboardService staffDashboardService;
    StationService stationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thêm trụ sạc mới vào trạm sạc",
            description = "Tạo thêm một trụ sạc mới cho trạm sạc đã tồn tại. Bao gồm các thông tin như loại đầu nối, công suất, giá cước. Chỉ quản trị viên có quyền thêm"
    )
    public ApiResponse<ChargingPointResponse> addChargingPoint(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId,
            @Valid @RequestBody ChargingPointCreationRequest request) {
        log.info("Admin adding charging point to station {}", stationId);
        return ApiResponse.<ChargingPointResponse>builder()
                .result(stationService.addChargingPointToStation(stationId, request))
                .message("Charging point created successfully")
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Lấy danh sách trụ sạc của một trạm",
            description = "Trả về danh sách tất cả các trụ sạc thuộc một trạm sạc cụ thể, bao gồm thông tin trạng thái, giá cước, công suất, loại đầu nối"
    )
    public ApiResponse<List<ChargingPointResponse>> getChargingPoints(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId) {
        log.info("Fetching charging points for station {}", stationId);
        return ApiResponse.<List<ChargingPointResponse>>builder()
                .result(stationService.getChargingPointsByStation(stationId))
                .build();
    }


    @PutMapping("/{chargingPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Cập nhật thông tin trụ sạc",
            description = "Cập nhật thông tin chi tiết của trụ sạc bao gồm trạng thái, giá cước, công suất, loại đầu nối. Admin và nhân viên trạm có quyền cập nhật"
    )
    public ApiResponse<ChargingPointResponse> updateChargingPoint(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId,
            @Parameter(description = "ID của trụ sạc", example = "CP_123")
            @PathVariable String chargingPointId,
            @Valid @RequestBody ChargingPointUpdateRequest request) {
        log.info("Updating charging point: {} at station: {}", chargingPointId, stationId);
        return ApiResponse.<ChargingPointResponse>builder()
                .result(stationService.updateChargingPoint(stationId, chargingPointId, request))
                .build();
    }

    @DeleteMapping("/{chargingPointId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa trụ sạc",
            description = "Xóa một trụ sạc khỏi trạm sạc theo ID. Chỉ quản trị viên có quyền xóa. Trụ sạc đã xóa sẽ không thể được sử dụng nữa"
    )
    public ApiResponse<Void> deleteChargingPoint(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId,
            @Parameter(description = "ID của trụ sạc", example = "CP_123")
            @PathVariable String chargingPointId) {
        log.info("Admin deleting charging point: {} from station: {}", chargingPointId, stationId);
        stationService.deleteChargingPoint(stationId, chargingPointId);
        return ApiResponse.<Void>builder()
                .message("Charging point deleted successfully")
                .build();
    }
}
