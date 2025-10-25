package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.ChargingPower;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationCreationRequest {
    @NotBlank(message = "STATION_NAME_REQUIRED")
    String name;

    @NotBlank(message = "STATION_ADDRESS_REQUIRED")
    String address;

    @NotNull(message = "STATION_CHARGING_POINTS_REQUIRED")
    @Min(value = 1, message = "STATION_CHARGING_POINTS_MIN")
    Integer numberOfChargingPoints;

    @NotNull(message = "STATION_POWER_OUTPUT_REQUIRED")
    ChargingPower powerOutput;

    String operatorName;
    String contactPhone;

    // Tọa độ là OPTIONAL - nếu không cung cấp, backend sẽ tự động geocode từ địa chỉ
    @DecimalMin(value = "-90.0", message = "LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "LATITUDE_INVALID")
    Double latitude;

    @DecimalMin(value = "-180.0", message = "LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "LONGITUDE_INVALID")
    Double longitude;

    String staffId; // ID của staff quản lý (optional)
}
