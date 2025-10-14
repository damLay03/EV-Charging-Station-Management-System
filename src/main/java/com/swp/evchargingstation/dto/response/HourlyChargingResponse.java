package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyChargingResponse {
    String hour;           // "06:00", "08:00", ...
    Integer sessionCount;  // Số phiên sạc trong giờ đó
    Double totalEnergy;    // Tổng năng lượng sạc
}

