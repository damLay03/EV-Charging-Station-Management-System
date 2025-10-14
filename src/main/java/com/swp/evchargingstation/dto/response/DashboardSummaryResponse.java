package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSummaryResponse {
    BigDecimal totalRevenue;        // Tổng chi phí
    Double totalEnergyUsed;         // Tổng năng lượng (kWh)
    Integer totalSessions;          // Số phiên sạc
    BigDecimal averagePricePerKwh;  // TB/kWh
}

