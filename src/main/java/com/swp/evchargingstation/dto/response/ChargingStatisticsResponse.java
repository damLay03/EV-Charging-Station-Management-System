package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingStatisticsResponse {
    Integer averageChargingTimeMinutes;  // Thời gian sạc trung bình
    String peakHours;                    // Giờ cao điểm (18:00 - 20:00)
    String mostFrequentDays;             // Ngày trong tuần thường sạc (Thứ 2, 6)
}

