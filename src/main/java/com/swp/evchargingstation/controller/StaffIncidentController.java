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
@RequestMapping("/api/my-stations/incidents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Staff Incidents", description = "Staff incident management - create, view, update incidents at their station")
public class StaffIncidentController {

    IncidentService incidentService;

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Create incident report",
               description = "Staff can report incidents at their station. Default status: WAITING")
    public ApiResponse<IncidentResponse> createIncident(@RequestBody @Valid IncidentCreationRequest request) {
        log.info("Staff creating incident report for station: {}", request.getStationId());
        return incidentService.createIncident(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get incidents of my station",
               description = "Staff can view all incidents reported at their station")
    public ApiResponse<List<IncidentResponse>> getMyStationIncidents() {
        log.info("Staff requesting incidents of their station");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(incidentService.getMyStationIncidents())
                .build();
    }

    @PatchMapping("/{incidentId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Update incident description",
               description = "Staff can update description of incidents at their station (cannot change status)")
    public ApiResponse<IncidentResponse> updateIncidentDescription(
            @PathVariable String incidentId,
            @RequestBody @Valid IncidentUpdateRequest request) {
        log.info("Staff updating incident {} description", incidentId);
        return incidentService.updateIncidentDescription(incidentId, request);
    }
}

