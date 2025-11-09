package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.entity.Booking;

import java.time.LocalDateTime;

public interface BookingService {
    BookingAvailabilityDto checkAvailability(String chargingPointId, LocalDateTime bookingTime, String vehicleId);
    Booking createBooking(BookingRequestDto bookingRequestDto, String userId);
    void processExpiredBookings();
}
