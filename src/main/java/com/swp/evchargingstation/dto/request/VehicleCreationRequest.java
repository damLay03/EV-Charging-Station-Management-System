package com.swp.evchargingstation.dto.request;

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

    @NotNull(message = "Model xe không được để trống")
    VehicleModel model;

    @NotBlank(message = "Giấy tờ xe không được để trống")
    String documentImageUrl; // Cloudinary URL của giấy tờ xe
}
