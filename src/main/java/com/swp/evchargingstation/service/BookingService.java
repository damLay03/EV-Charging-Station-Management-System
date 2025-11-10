package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.dto.BookingResponseDto;
import com.swp.evchargingstation.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    BookingAvailabilityDto checkAvailability(String chargingPointId, LocalDateTime bookingTime, String vehicleId);
    BookingResponseDto createBooking(BookingRequestDto bookingRequestDto, String email);
    BookingResponseDto getBookingById(Long bookingId, String email);
    List<BookingResponseDto> getUserBookings(String email);
    BookingResponseDto cancelBooking(Long bookingId, String email);
    BookingResponseDto checkInBooking(Long bookingId, String email);
    void processExpiredBookings();
}
