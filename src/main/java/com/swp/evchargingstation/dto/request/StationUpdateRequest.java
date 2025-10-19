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
    @NotBlank(message = "Tên trạm không được để trống")
    String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    String address;

    String operatorName;

    String contactPhone;

    // Tọa độ là OPTIONAL - nếu không cung cấp, backend sẽ tự động geocode từ địa chỉ
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    Double longitude;

    @NotNull(message = "Trạng thái không được để trống")
    StationStatus status;

    String staffId; // ID của staff quản lý (optional, null để bỏ gán)
}
