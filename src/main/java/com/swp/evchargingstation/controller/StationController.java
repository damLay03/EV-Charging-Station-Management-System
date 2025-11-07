package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ChargingPointCreationRequest;
import com.swp.evchargingstation.dto.request.ChargingPointUpdateRequest;
import com.swp.evchargingstation.dto.request.StationCreationRequest;
import com.swp.evchargingstation.dto.request.StationUpdateRequest;
import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.enums.StationStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Stations Management", description = "RESTful API quản lý trạm sạc và trụ sạc (Charging Points) - Phân quyền theo role")
public class StationController {
    StationService stationService;
    StaffDashboardService staffDashboardService;

    @GetMapping
    @Operation(
            summary = "[ALL] Lấy danh sách trạm sạc",
            description = "Trả về danh sách các trạm sạc theo view được chỉ định. " +
                    "view=basic (mặc định): thông tin cơ bản, " +
                    "view=overview: tổng quan, " +
                    "view=detail: chi tiết đầy đủ. " +
                    "Có thể lọc theo trạng thái (OPERATIONAL, OUT_OF_SERVICE, MAINTENANCE, CLOSED)"
    )
    public ApiResponse<?> list(
            @Parameter(description = "Mức độ chi tiết (basic, overview, detail)", example = "basic")
            @RequestParam(value = "view", defaultValue = "basic") String view,
            @Parameter(description = "Lọc theo trạng thái", example = "OPERATIONAL")
            @RequestParam(value = "status", required = false) StationStatus status) {
        log.info("Fetching stations - view: {}, status: {}", view, status);

        return switch (view.toLowerCase()) {
            case "overview" -> ApiResponse.<List<StationOverviewResponse>>builder()
                    .result(stationService.getAllOverview())
                    .build();
            case "detail" -> ApiResponse.<List<StationDetailResponse>>builder()
                    .result(stationService.getStationsWithDetail(status))
                    .build();
            case "basic" -> ApiResponse.<List<StationResponse>>builder()
                    .result(stationService.getStations(status))
                    .build();
            default -> throw new IllegalArgumentException("Invalid view: " + view);
        };
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
            summary = "[ADMIN] Tạo trạm sạc mới",
            description = "Tạo một trạm sạc mới với các thông tin cơ bản như tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ"
    )
    public ApiResponse<StationResponse> create(@Valid @RequestBody StationCreationRequest request) {
        log.info("Admin creating new station: {}", request.getName());
        return ApiResponse.<StationResponse>builder()
                .result(stationService.createStation(request))
                .build();
    }

    @PutMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Cập nhật thông tin trạm sạc",
            description = "Cập nhật thông tin chi tiết của trạm sạc bao gồm tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ, và trạng thái"
    )
    public ApiResponse<StationResponse> update(@PathVariable String stationId, @Valid @RequestBody StationUpdateRequest request) {
        log.info("Admin updating station: {}", stationId);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStation(stationId, request))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa trạm sạc",
            description = "Xóa một trạm sạc khỏi hệ thống theo ID. Các trụ sạc liên quan sẽ tự động bị xóa (cascade delete). Chỉ quản trị viên có quyền xóa"
    )
    public ApiResponse<Void> delete(@PathVariable String stationId) {
        log.info("Admin deleting station: {}", stationId);
        stationService.deleteStation(stationId);
        return ApiResponse.<Void>builder()
                .message("Station deleted successfully")
                .build();
    }

    // ==================== CHARGING POINTS MANAGEMENT ====================

    // ==================== STAFF - MY STATION CHARGING POINTS ====================

    @GetMapping("/my-station/charging-points")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Lấy danh sách trụ sạc tại trạm của tôi",
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

    // ==================== ADMIN/STAFF - CHARGING POINTS CRUD ====================

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{stationId}/charging-points")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Thêm trụ sạc mới vào trạm sạc",
            description = "Tạo thêm một trụ sạc mới cho trạm sạc đã tồn tại. Bao gồm các thông tin như loại đầu nối, công suất, giá cước"
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

    @GetMapping("/{stationId}/charging-points")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "[ALL] Lấy danh sách trụ sạc của một trạm",
            description = "Trả về danh sách tất cả các trụ sạc thuộc một trạm sạc cụ thể"
    )
    public ApiResponse<List<ChargingPointResponse>> getChargingPoints(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId) {
        log.info("Fetching charging points for station {}", stationId);
        return ApiResponse.<List<ChargingPointResponse>>builder()
                .result(stationService.getChargingPointsByStation(stationId))
                .build();
    }

    @PutMapping("/{stationId}/charging-points/{chargingPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "[ADMIN/STAFF] Cập nhật thông tin trụ sạc",
            description = "Cập nhật thông tin chi tiết của trụ sạc bao gồm trạng thái, giá cước, công suất, loại đầu nối"
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

    @DeleteMapping("/{stationId}/charging-points/{chargingPointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa trụ sạc",
            description = "Xóa một trụ sạc theo ID. Chỉ quản trị viên có quyền xóa"
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






