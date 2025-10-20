package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingSimulationResponse {
    String sessionId;
    String message;
    int currentSocPercent;
    float energyDeliveredKwh;
    float currentCost;
    boolean isCompleted;
}


