package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.enums.VehicleModel;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for staff to lookup vehicle information by license plate
 * Contains all necessary information to start a charging session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleLookupResponse {
    // Vehicle information
    String vehicleId;
    String licensePlate;
    VehicleModel model;
    String modelName;
    VehicleBrand brand;
    String brandDisplayName;
    Integer currentSocPercent;
    Float batteryCapacityKwh;
    String batteryType;
    String maxChargingPower;
    Float maxChargingPowerKw;

    // Driver/Owner information
    String ownerId;
    String ownerName;
    String ownerEmail;
    String ownerPhone;

    // Additional info for charging session
    Boolean hasActiveSession;
    String activeSessionId;
}

