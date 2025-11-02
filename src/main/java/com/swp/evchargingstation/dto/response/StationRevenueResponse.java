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
    Integer day;    // Ngày trong tháng (1-31)
    Integer month;
    Integer year;
    Integer week;
    Float totalRevenue;
    Integer totalSessions;
}