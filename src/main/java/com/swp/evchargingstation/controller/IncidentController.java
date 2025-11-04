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
@Tag(name = "Incidents (Admin)", description = "Admin incident management - view, update status, delete incidents")
public class IncidentController {

    IncidentService incidentService;

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

    @PatchMapping("/{incidentId}")
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

