package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.entity.Booking;
import com.swp.evchargingstation.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/availability")
    public ResponseEntity<BookingAvailabilityDto> checkAvailability(
            @RequestParam Long chargingPointId,
            @RequestParam LocalDateTime bookingTime,
            @RequestParam Long vehicleId) {
        return ResponseEntity.ok(bookingService.checkAvailability(chargingPointId, bookingTime, vehicleId));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequestDto bookingRequestDto, @AuthenticationPrincipal UserDetails userDetails) {
        // Assuming userDetails.getUsername() returns the user ID as a string
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(bookingService.createBooking(bookingRequestDto, userId));
    }
}

