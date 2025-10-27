package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.CashPaymentRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CashPaymentRequestResponse {
    String requestId;
    String paymentId;
    String sessionId;

    // Thông tin driver
    String driverId;
    String driverName;
    String driverPhone;

    // Thông tin session sạc
    String stationName;
    String chargingPointName;
    LocalDateTime sessionStartTime;
    LocalDateTime sessionEndTime;
    Float energyKwh;

    // Thông tin thanh toán
    Float amount;
    CashPaymentRequestStatus status;
    LocalDateTime createdAt;
    LocalDateTime confirmedAt;
    String confirmedByStaffName;

    // Thông tin xe
    String vehicleModel;
    String licensePlate;
}
