package com.swp.evchargingstation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {
    private String vehicleId;
    private String chargingPointId;
    private LocalDateTime bookingTime;
    private Float desiredPercentage;
}

