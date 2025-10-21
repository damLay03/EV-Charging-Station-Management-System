package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingPower;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingPointCreationRequest {
    @NotNull(message = "Công suất không được để trống")
    ChargingPower chargingPower;

    ChargingPointStatus status; // Optional - mặc định sẽ là AVAILABLE nếu không truyền
}
