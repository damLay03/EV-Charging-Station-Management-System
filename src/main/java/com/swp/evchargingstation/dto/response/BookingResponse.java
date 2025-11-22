package com.swp.evchargingstation.dto.response;

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
public class BookingResponse {
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

    // ✅ Fields để frontend auto-start session
    private String chargingPointId;  // Cần cho startSession request
    private String vehicleId;         // ✅ String vì Vehicle.vehicleId là String (UUID)
    private Integer currentSocPercent; // ✅ SOC hiện tại của xe
}

