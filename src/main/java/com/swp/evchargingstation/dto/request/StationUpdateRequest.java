package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.StationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Trạng thái không được để trống")
    StationStatus status;
}

