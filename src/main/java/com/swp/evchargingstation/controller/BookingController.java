package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.request.BookingRequest;
import com.swp.evchargingstation.dto.response.BookingResponse;
import com.swp.evchargingstation.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "APIs for managing charging station bookings")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Check booking availability",
               description = "Check if a charging point is available at specified time and calculate max charge percentage")
    @GetMapping("/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingAvailabilityDto> checkAvailability(
            @RequestParam String chargingPointId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingTime,
            @RequestParam String vehicleId) {
        return ResponseEntity.ok(bookingService.checkAvailability(chargingPointId, bookingTime, vehicleId));
    }

    @Operation(summary = "Create new booking",
               description = "Create a new charging station booking with deposit payment")
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest bookingRequest,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject(); // Lấy email từ "sub" claim
        return ResponseEntity.ok(bookingService.createBooking(bookingRequest, email));
    }

    @Operation(summary = "Get booking by ID",
               description = "Get detailed information about a specific booking")
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, email));
    }

    @Operation(summary = "Get user's bookings",
               description = "Get all bookings for the authenticated user")
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getUserBookings(email));
    }

    @Operation(summary = "Cancel booking",
               description = "Cancel a confirmed booking and get deposit refund")
    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, email));
    }

    @Operation(summary = "Check-in to booking",
               description = "Check-in to start charging session (must be within 15 minutes of booking time)")
    @PutMapping("/{bookingId}/check-in")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> checkInBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaim("userId"); // ✅ Fix: Lấy userId thay vì email
        return ResponseEntity.ok(bookingService.checkInBooking(bookingId, userId));
    }
}


