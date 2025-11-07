package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.IncidentCreationRequest;
import com.swp.evchargingstation.dto.request.IncidentUpdateRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.IncidentResponse;
import com.swp.evchargingstation.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Incidents Management", description = "RESTful API quản lý sự cố - Phân quyền theo role")
public class IncidentController {

    IncidentService incidentService;

    // ==================== STAFF ENDPOINTS ====================

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Tạo báo cáo sự cố",
            description = "Staff có thể báo cáo sự cố tại trạm của họ. Trạng thái mặc định: WAITING"
    )
    public ApiResponse<IncidentResponse> createIncident(@RequestBody @Valid IncidentCreationRequest request) {
        log.info("Staff creating incident report for station: {}", request.getStationId());
        return incidentService.createIncident(request);
    }

    @GetMapping("/my-station")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Lấy danh sách sự cố tại trạm của tôi",
            description = "Staff có thể xem tất cả sự cố được báo cáo tại trạm của họ"
    )
    public ApiResponse<List<IncidentResponse>> getMyStationIncidents() {
        log.info("Staff requesting incidents of their station");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(incidentService.getMyStationIncidents())
                .build();
    }

    @PatchMapping("/{incidentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "[STAFF/ADMIN] Cập nhật sự cố",
            description = "Staff: chỉ có thể cập nhật description của sự cố tại trạm của họ. " +
                         "Admin: có thể cập nhật cả description và status (WAITING -> WORKING -> DONE)"
    )
    public ApiResponse<IncidentResponse> updateIncident(
            @PathVariable String incidentId,
            @RequestBody @Valid IncidentUpdateRequest request) {
        log.info("Updating incident {}", incidentId);
        return incidentService.updateIncident(incidentId, request);
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy tất cả sự cố",
            description = "Admin có thể xem tất cả sự cố từ tất cả các trạm"
    )
    public ApiResponse<List<IncidentResponse>> getAllIncidents() {
        log.info("Admin requesting all incidents");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(incidentService.getAllIncidents())
                .build();
    }

    @GetMapping("/{incidentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy chi tiết sự cố",
            description = "Admin có thể xem thông tin chi tiết của bất kỳ sự cố nào"
    )
    public ApiResponse<IncidentResponse> getIncidentById(@PathVariable String incidentId) {
        log.info("Admin requesting incident: {}", incidentId);
        return incidentService.getIncidentById(incidentId);
    }
    @ResponseStatus(HttpStatus.NO_CONTENT)

    @DeleteMapping("/{incidentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa sự cố",
            description = "Admin có thể xóa bất kỳ báo cáo sự cố nào"
    )
    public ApiResponse<Void> deleteIncident(@PathVariable String incidentId) {
        log.info("Admin deleting incident: {}", incidentId);
        return incidentService.deleteIncident(incidentId);
    }
}

