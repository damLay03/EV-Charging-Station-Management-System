package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingPower;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingPointUpdateRequest {
    ChargingPower chargingPower;

    ChargingPointStatus status;
}
