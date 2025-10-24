package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffChargingPointResponse {
    String pointId;
    String name; // TS1, TS2, ...
    float maxPowerKw;
    ChargingPointStatus status;

    // Thông tin session hiện tại nếu đang sạc
    String currentSessionId;
    String driverName;
    String vehicleModel;
    String startTime;
    int currentSocPercent;
}
