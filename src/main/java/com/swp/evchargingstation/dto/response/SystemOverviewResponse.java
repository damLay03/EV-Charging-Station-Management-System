package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemOverviewResponse {
    long totalStations;
    long totalChargingPoints; // Tổng số trụ sạc
    long activeChargingPoints; // Số trụ sạc đang hoạt động (AVAILABLE + CHARGING)
    long totalDrivers;
    float currentMonthRevenue;
}