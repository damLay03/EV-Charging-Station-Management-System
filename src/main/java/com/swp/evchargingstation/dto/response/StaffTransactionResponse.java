package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingSessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffTransactionResponse {
    String sessionId;
    String driverName;
    String driverPhone;
    String vehicleModel;
    String chargingPointId;
    LocalDateTime startTime;
    LocalDateTime endTime;
    float energyKwh;
    float durationMin;
    float costTotal;
    ChargingSessionStatus status;
    boolean isPaid;
}

