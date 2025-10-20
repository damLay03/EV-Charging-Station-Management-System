package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingSessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingProgressResponse {
    String sessionId;
    String vehicleId;
    String vehicleLicensePlate;
    String vehicleModel;
    String chargingPointId;
    String stationName;
    float maxPowerKw;

    LocalDateTime startTime;
    int startSocPercent;
    int currentSocPercent;
    int targetSocPercent;

    float energyDeliveredKwh;
    float estimatedTotalEnergyKwh;
    int progressPercent;
    int elapsedTimeMinutes;
    int estimatedRemainingMinutes;

    float currentCost;
    float estimatedTotalCost;
    float pricePerKwh;

    ChargingSessionStatus status;
    String statusMessage;
}

