package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingPower;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingPointResponse {
    String pointId;
    String name; // TS1, TS2, ...
    String stationId;
    String stationName;
    ChargingPower chargingPower;
    ChargingPointStatus status; // Trạng thái vật lý thực tế của trụ
    ChargingPointStatus displayStatus; // Trạng thái hiển thị động (bao gồm RESERVED nếu có booking sắp tới)
    String currentSessionId;

    // Thông tin booking sắp tới (nếu có)
    Long upcomingBookingId;
    LocalDateTime upcomingBookingTime;
    String upcomingBookingUserName;
}
