package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.enums.VehicleModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleCreationRequest {
    @NotBlank(message = "Biển số xe không được để trống")
    String licensePlate;

    @NotNull(message = "Hãng xe không được để trống")
    VehicleBrand brand;

    @NotNull(message = "Model xe không được để trống")
    VehicleModel model;
}
