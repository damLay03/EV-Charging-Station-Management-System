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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Incident Management", description = "APIs for managing incident reports")
public class IncidentController {

    IncidentService incidentService;

//=====================================================STAFF============================================================

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Create incident report",
               description = "Staff can report incidents at their station. Default status: WAITING")
    public ApiResponse<IncidentResponse> createIncident(@RequestBody @Valid IncidentCreationRequest request) {
        log.info("Staff creating incident report for station: {}", request.getStationId());
        return incidentService.createIncident(request);
    }

    @GetMapping("/my-station")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get incidents of my station",
               description = "Staff can view all incidents reported at their station")
    public ApiResponse<List<IncidentResponse>> getMyStationIncidents() {
        log.info("Staff requesting incidents of their station");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(incidentService.getMyStationIncidents())
                .build();
    }

    @PutMapping("/{incidentId}/description")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Update incident description",
               description = "Staff can update description of incidents at their station (cannot change status)")
    public ApiResponse<IncidentResponse> updateIncidentDescription(
            @PathVariable String incidentId,
            @RequestBody @Valid IncidentUpdateRequest request) {
        log.info("Staff updating incident {} description", incidentId);
        return incidentService.updateIncidentDescription(incidentId, request);
    }

//=====================================================ADMIN============================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all incidents",
               description = "Admin can view all incidents from all stations")
    public ApiResponse<List<IncidentResponse>> getAllIncidents() {
        log.info("Admin requesting all incidents");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(incidentService.getAllIncidents())
                .build();
    }

    @GetMapping("/{incidentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get incident by ID",
               description = "Admin can view detailed information of any incident")
    public ApiResponse<IncidentResponse> getIncidentById(@PathVariable String incidentId) {
        log.info("Admin requesting incident: {}", incidentId);
        return incidentService.getIncidentById(incidentId);
    }

    @PutMapping("/{incidentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update incident status",
               description = "Admin can update incident status: WAITING -> WORKING -> DONE")
    public ApiResponse<IncidentResponse> updateIncidentStatus(
            @PathVariable String incidentId,
            @RequestBody @Valid IncidentUpdateRequest request) {
        log.info("Admin updating incident {} to status: {}", incidentId, request.getStatus());
        return incidentService.updateIncidentStatus(incidentId, request);
    }

    @DeleteMapping("/{incidentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete incident",
               description = "Admin can delete any incident report")
    public ApiResponse<Void> deleteIncident(@PathVariable String incidentId) {
        log.info("Admin deleting incident: {}", incidentId);
        return incidentService.deleteIncident(incidentId);
    }
}

