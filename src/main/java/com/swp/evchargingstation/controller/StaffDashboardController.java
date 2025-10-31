package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.IncidentCreationRequest;
import com.swp.evchargingstation.dto.request.IncidentUpdateRequest;
import com.swp.evchargingstation.dto.request.StaffPaymentRequest;
import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.service.StaffDashboardService;
import com.swp.evchargingstation.service.StationService;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Staff Dashboard", description = "APIs for staff to manage their station")
public class StaffDashboardController {

    StaffDashboardService staffDashboardService;
    StationService stationService;
    VehicleService vehicleService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get staff dashboard overview",
               description = "Get today's sessions count, revenue, and station charging points status")
    public ApiResponse<StaffDashboardResponse> getStaffDashboard() {
        log.info("Staff requesting dashboard overview");
        return ApiResponse.<StaffDashboardResponse>builder()
                .result(staffDashboardService.getStaffDashboard())
                .build();
    }

    @GetMapping("/charging-points")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get charging points of staff's station",
               description = "Get all charging points with current session info if available")
    public ApiResponse<List<StaffChargingPointResponse>> getChargingPoints() {
        log.info("Staff requesting charging points list");
        return ApiResponse.<List<StaffChargingPointResponse>>builder()
                .result(staffDashboardService.getStaffChargingPoints())
                .build();
    }

    @GetMapping("/my-station/charging-points")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "List charging points at my station",
               description = "Return empty list if staff is not assigned to any station")
    public ApiResponse<List<ChargingPointResponse>> getMyStationChargingPoints(@AuthenticationPrincipal Jwt jwt) {
        String staffId = jwt.getClaim("userId");
        log.info("Staff {} requesting all charging points at their station", staffId);
        return ApiResponse.<List<ChargingPointResponse>>builder()
                .result(stationService.getMyStationChargingPoints(staffId))
                .build();
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get transactions of staff's station",
               description = "Get all charging sessions for payment processing")
    public ApiResponse<List<StaffTransactionResponse>> getTransactions() {
        log.info("Staff requesting transactions list");
        return ApiResponse.<List<StaffTransactionResponse>>builder()
                .result(staffDashboardService.getStaffTransactions())
                .build();
    }

    @PostMapping("/process-payment")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Process payment for driver",
               description = "Staff can process cash or card payment for completed charging sessions")
    public ApiResponse<String> processPayment(@RequestBody StaffPaymentRequest request) {
        log.info("Staff processing payment for session: {}", request.getSessionId());
        return staffDashboardService.processPaymentForDriver(request);
    }

    @GetMapping("/incidents")
//    @PreAuthorize("hasRole('STAFF')")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get incidents of staff's station",
               description = "Get all incidents reported for the station")
    public ApiResponse<List<IncidentResponse>> getIncidents() {
        log.info("Staff requesting incidents list");
        return ApiResponse.<List<IncidentResponse>>builder()
                .result(staffDashboardService.getStaffIncidents())
                .build();
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Create incident report",
               description = "Staff can report incidents at their station")
    public ApiResponse<IncidentResponse> createIncident(@RequestBody IncidentCreationRequest request) {
        log.info("Staff creating incident report for station: {}", request.getStationId());
        return staffDashboardService.createIncident(request);
    }

    @PutMapping("/incidents/{incidentId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Update incident status",
               description = "Staff can update incident status and resolution")
    public ApiResponse<IncidentResponse> updateIncident(
            @PathVariable String incidentId,
            @RequestBody IncidentUpdateRequest request) {
        log.info("Staff updating incident: {} to status: {}", incidentId, request.getStatus());
        return staffDashboardService.updateIncident(incidentId, request);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get my staff profile",
               description = "Return staff info and the station currently assigned (if any)")
    public ApiResponse<StaffProfileResponse> getMyProfile() {
        return ApiResponse.<StaffProfileResponse>builder()
                .result(staffDashboardService.getMyProfile())
                .build();
    }

    @GetMapping("/pending-payments")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get pending cash payments",
               description = "Get all cash payments awaiting staff confirmation at this station")
    public ApiResponse<List<PendingPaymentResponse>> getPendingPayments(@AuthenticationPrincipal Jwt jwt) {
        String staffId = jwt.getClaim("userId");
        log.info("Staff {} requesting pending cash payments", staffId);
        return ApiResponse.<List<PendingPaymentResponse>>builder()
                .result(staffDashboardService.getPendingCashPayments(staffId))
                .build();
    }

    @PostMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Confirm cash payment",
               description = "Staff confirms that driver has paid in cash")
    public ApiResponse<String> confirmCashPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {
        String staffId = jwt.getClaim("userId");
        log.info("Staff {} confirming cash payment {}", staffId, paymentId);
        return staffDashboardService.confirmCashPayment(paymentId, staffId);
    }

    @GetMapping("/vehicles/lookup")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Lookup vehicle by license plate",
               description = "Staff enters license plate to get all vehicle and owner information needed to start charging session")
    public ApiResponse<VehicleLookupResponse> lookupVehicleByLicensePlate(
            @RequestParam String licensePlate) {
        log.info("Staff looking up vehicle with license plate: {}", licensePlate);
        return ApiResponse.<VehicleLookupResponse>builder()
                .result(vehicleService.lookupVehicleByLicensePlate(licensePlate))
                .build();
    }
}
