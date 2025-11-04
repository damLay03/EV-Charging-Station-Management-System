package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.StationCreationRequest;
import com.swp.evchargingstation.dto.request.StationUpdateRequest;
import com.swp.evchargingstation.dto.response.StationDetailResponse;
import com.swp.evchargingstation.dto.response.StationOverviewResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.enums.StationStatus;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Stations", description = "Quản lý trạm sạc")
public class StationController {
    StationService stationService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách trạm sạc",
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo trạm sạc mới",
            description = "Tạo một trạm sạc mới với các thông tin cơ bản như tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ. Chỉ quản trị viên có quyền tạo"
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
            summary = "Cập nhật thông tin trạm sạc",
            description = "Cập nhật thông tin chi tiết của trạm sạc bao gồm tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ, và trạng thái. Chỉ quản trị viên có quyền cập nhật"
    )
    public ApiResponse<StationResponse> update(@PathVariable String stationId, @Valid @RequestBody StationUpdateRequest request) {
        log.info("Admin updating station: {}", stationId);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStation(stationId, request))
                .build();
    }

    @DeleteMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa trạm sạc",
            description = "Xóa một trạm sạc khỏi hệ thống theo ID. Các trụ sạc liên quan sẽ tự động bị xóa (cascade delete). Chỉ quản trị viên có quyền xóa"
    )
    public ApiResponse<Void> delete(@PathVariable String stationId) {
        log.info("Admin deleting station: {}", stationId);
        stationService.deleteStation(stationId);
        return ApiResponse.<Void>builder()
                .message("Station deleted successfully")
                .build();
    }
}






