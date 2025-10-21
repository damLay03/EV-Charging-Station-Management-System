package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartChargingRequest {
    @NotBlank(message = "Vehicle ID is required")
    String vehicleId;

    @NotBlank(message = "Charging Point ID is required")
    String chargingPointId;


    @Min(value = 1, message = "Target SOC must be between 1 and 100")
    @Max(value = 100, message = "Target SOC must be between 1 and 100")
    int targetSocPercent;
}