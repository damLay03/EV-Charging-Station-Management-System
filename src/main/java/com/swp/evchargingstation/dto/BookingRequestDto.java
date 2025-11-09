package com.swp.evchargingstation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {
    private Long vehicleId;
    private Long chargingPointId;
    private LocalDateTime bookingTime;
    private Float desiredPercentage;
}

