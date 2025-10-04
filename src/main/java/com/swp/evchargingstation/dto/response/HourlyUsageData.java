package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyUsageData {
    Integer hour; // 0-23
    Integer sessionCount; // Số session trong giờ này
    Integer activePoints; // Số điểm đang sạc trong giờ này
    Double usagePercent; // % sử dụng trong giờ này
    Double energyConsumed; // kWh tiêu thụ trong giờ này
    Double revenue; // Doanh thu trong giờ này
}