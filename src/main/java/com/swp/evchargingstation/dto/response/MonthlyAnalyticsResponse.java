package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyAnalyticsResponse {
    Integer month;              // Tháng (6, 7, 8, 9, 10)
    Integer year;               // Năm (2025)
    Double totalCost;           // Tổng chi phí trong tháng
    Double totalEnergyKwh;      // Tổng năng lượng tiêu thụ (kWh)
    Integer totalSessions;      // Số phiên sạc trong tháng
    String monthLabel;          // Label hiển thị "T6", "T7", "T8", "T9", "T10"
}

