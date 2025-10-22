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
    @NotBlank(message = "Charging Point ID is required")
    String chargingPointId;

    @NotBlank(message = "Vehicle ID is required")
    String vehicleId;

    // Optional, default to 100 if null in service
    Integer targetSocPercent;
}

