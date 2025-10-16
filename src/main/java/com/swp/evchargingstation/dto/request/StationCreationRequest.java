package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationCreationRequest {
    @NotBlank(message = "Tên trạm không được để trống")
    String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    String address;

    @NotNull(message = "Số điểm sạc không được để trống")
    @Min(value = 1, message = "Số điểm sạc phải lớn hơn 0")
    Integer numberOfChargingPoints;

    @NotNull(message = "Công suất không được để trống")
    @Min(value = 1, message = "Công suất phải lớn hơn 0")
    Float powerOutputKw;

    String operatorName;
    String contactPhone;
    String staffId; // ID của staff quản lý (optional)
}
