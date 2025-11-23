package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.enums.VehicleModel;
import com.swp.evchargingstation.enums.VehicleRegistrationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    Integer currentSocPercent;

    // Driver/Owner information
    String ownerName;        // Tên đầy đủ của driver
    String ownerEmail;       // Email của driver
    String ownerPhone;       // Số điện thoại của driver

    // Approval fields - 3 ảnh bắt buộc
    String documentFrontImageUrl; // Ảnh mặt trước cà vẹt
    String documentBackImageUrl;  // Ảnh mặt sau cà vẹt
    String frontImageUrl;         // Ảnh xe có biển số rõ ràng
    VehicleRegistrationStatus approvalStatus;
    String rejectionReason;
    LocalDateTime submittedAt;
    LocalDateTime approvedAt;
    String approvedByAdminId;
    String approvedByAdminName;

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

    public String getMaxChargingPower() {
        return model != null ? model.getMaxChargingPower() : null;
    }

    public float getMaxChargingPowerKw() {
        return model != null ? model.getMaxChargingPowerKw() : 0f;
    }

    public String getImageUrl() {
        return model != null ? model.getImageUrl() : null;
    }
}
