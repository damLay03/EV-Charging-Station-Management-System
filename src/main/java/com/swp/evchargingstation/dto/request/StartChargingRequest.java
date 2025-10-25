package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartChargingRequest {
    @NotBlank(message = "CHARGING_POINT_ID_REQUIRED")
    String chargingPointId;

    @NotBlank(message = "VEHICLE_ID_REQUIRED")
    String vehicleId;

    // Optional, default to 100 if null in service
    Integer targetSocPercent;
}
