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
public class VehicleModelResponse {
    VehicleModel model;
    String modelName;
    VehicleBrand brand;
    float batteryCapacityKwh;
    String batteryType;
}
