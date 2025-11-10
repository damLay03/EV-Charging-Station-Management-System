package com.swp.evchargingstation.dto;

import com.swp.evchargingstation.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private String userName;
    private String userEmail;
    private String vehicleModel;
    private String vehicleLicensePlate;
    private String chargingPointName;
    private String stationName;
    private String stationAddress;
    private LocalDateTime bookingTime;
    private LocalDateTime estimatedEndTime;
    private Float desiredPercentage;
    private Double depositAmount;
    private BookingStatus bookingStatus;
    private LocalDateTime createdAt;
}

