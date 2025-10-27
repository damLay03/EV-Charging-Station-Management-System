package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveChargingSessionResponse {
    // Thông tin trạm sạc
    String stationName;
    String chargingPointName;
    String stationLocation;

    // Trạng thái sạc
    String status; // IN_PROGRESS, COMPLETED, etc.
    int currentSocPercent; // Mức pin hiện tại
    int targetSocPercent; // Mục tiêu
    int startSocPercent; // Mức pin ban đầu

    // Thời gian
    String startTime; // Thời gian bắt đầu
    int elapsedTimeMinutes; // Thời gian đã sạc (phút)
    Integer estimatedTimeRemainingMinutes; // Thời gian còn lại ước tính (phút)

    // Chi phí
    float pricePerKwh; // Giá điện theo plan
    float energyConsumedKwh; // Năng lượng đã tiêu thụ (kWh)
    float currentCost; // Chi phí hiện tại

    // Thông tin công suất
    String powerOutput; // Công suất đầu ra (ví dụ: "N/A kW" hoặc "50 kW")

    // Session ID để frontend có thể dừng sạc
    String sessionId;
}

