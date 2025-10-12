package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleCreationRequest {
    @NotBlank(message = "Biển số xe không được để trống")
    String licensePlate;

    @NotBlank(message = "Model xe không được để trống")
    String model;

    @Positive(message = "Dung lượng pin phải lớn hơn 0")
    float batteryCapacityKwh;

    String batteryType;
}

