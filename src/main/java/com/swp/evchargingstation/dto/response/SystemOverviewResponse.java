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
    long activeChargingPoints;
    long totalDrivers;
    float currentMonthRevenue;
}