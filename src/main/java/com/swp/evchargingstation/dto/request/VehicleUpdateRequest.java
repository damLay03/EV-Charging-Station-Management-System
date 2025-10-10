package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleUpdateRequest {
    // NOTE: Tất cả field đều OPTIONAL cho partial update
    String licensePlate;  // optional - có thể cập nhật biển số
    String model;         // optional
    Float batteryCapacityKwh; // optional - dùng Float để có thể null
    String batteryType;   // optional
}
