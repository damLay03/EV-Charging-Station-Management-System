package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.StationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationUpdateRequest {
    @NotBlank(message = "STATION_NAME_REQUIRED")
    String name;

    @NotBlank(message = "STATION_ADDRESS_REQUIRED")
    String address;

    String operatorName;

    String contactPhone;

    // Tọa độ là OPTIONAL - nếu không cung cấp, backend sẽ tự động geocode từ địa chỉ
    @DecimalMin(value = "-90.0", message = "LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "LATITUDE_INVALID")
    Double latitude;

    @DecimalMin(value = "-180.0", message = "LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "LONGITUDE_INVALID")
    Double longitude;

    @NotNull(message = "STATION_STATUS_REQUIRED")
    StationStatus status;

    String staffId; // ID của staff quản lý (optional, null để bỏ gán)
}
