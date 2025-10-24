package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingPower;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingPointResponse {
    String pointId;
    String name; // TS1, TS2, ...
    String stationId;
    String stationName;
    ChargingPower chargingPower;
    ChargingPointStatus status;
    String currentSessionId;
}
