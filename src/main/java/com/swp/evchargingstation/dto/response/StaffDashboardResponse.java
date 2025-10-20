package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffDashboardResponse {
    // Tổng quan hôm nay
    int todaySessionsCount;
    double todayRevenue;
    Double averageSessionDuration; // minutes, có thể null nếu không có session

    // Thông tin trạm
    String stationId;
    String stationName;
    String stationAddress;
    int totalChargingPoints;
    int availablePoints;
    int chargingPoints;
    int offlinePoints;
}

