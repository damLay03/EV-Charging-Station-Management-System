package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PendingPaymentResponse {
    String paymentId;
    String sessionId;
    String driverName;
    String driverPhone;
    String licensePlate;
    LocalDateTime sessionStartTime;
    LocalDateTime sessionEndTime;
    Integer durationMin;
    Float energyKwh;
    Float amount;
    String paymentStatus;
    LocalDateTime requestedAt;
    String stationName;
    String chargingPointName;
}

