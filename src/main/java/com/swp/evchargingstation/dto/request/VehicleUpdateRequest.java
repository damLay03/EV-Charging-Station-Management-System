package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.VehicleModel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleUpdateRequest {
    // NOTE: Tất cả field đều OPTIONAL cho partial update
    String licensePlate;  // optional - có thể cập nhật biển số
    VehicleModel model;   // optional
}
