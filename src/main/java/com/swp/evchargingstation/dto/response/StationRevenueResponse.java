package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationRevenueResponse {
    String stationId;
    String stationName;
    String address;
    int month;
    int year;
    int week;
    float totalRevenue;
    int totalSessions;
}