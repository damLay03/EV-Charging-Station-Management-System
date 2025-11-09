package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.entity.Booking;

import java.time.LocalDateTime;

public interface BookingService {
    BookingAvailabilityDto checkAvailability(Long chargingPointId, LocalDateTime bookingTime, Long vehicleId);
    Booking createBooking(BookingRequestDto bookingRequestDto, Long userId);
    void processExpiredBookings();
}
