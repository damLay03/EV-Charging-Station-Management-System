package com.swp.evchargingstation.dto.response.driver;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverDashboardResponse {
    // Thông tin tổng quan
    Double totalCost;           // Tổng chi phí (727,690đ)
    Double totalEnergyKwh;      // Tổng năng lượng (212.9 kWh)
    Integer totalSessions;       // Số phiên sạc (5)
    String averageCostPerMonth;  // TB/tháng (3418đ)

    // Thông tin xe và pin
    String vehicleModel;         // Tesla Model 3
    String licensePlate;         // Biển số: 30A-12345
    Integer currentBatterySoc;   // % pin hiện tại: 75%
}
