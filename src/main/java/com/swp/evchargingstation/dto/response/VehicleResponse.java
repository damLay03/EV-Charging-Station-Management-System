package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.enums.VehicleModel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleResponse {
    String vehicleId;
    String licensePlate;
    VehicleModel model;
    String ownerId;

    // Computed fields from model enum
    public VehicleBrand getBrand() {
        return model != null ? model.getBrand() : null;
    }

    public String getBrandDisplayName() {
        return model != null && model.getBrand() != null ? model.getBrand().getDisplayName() : null;
    }

    public String getModelName() {
        return model != null ? model.getModelName() : null;
    }

    public float getBatteryCapacityKwh() {
        return model != null ? model.getBatteryCapacityKwh() : 0f;
    }

    public String getBatteryType() {
        return model != null ? model.getBatteryType() : null;
    }
}
