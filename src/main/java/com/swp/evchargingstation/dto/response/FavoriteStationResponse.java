package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoriteStationResponse {
    String stationId;
    String stationName;
    String address;
    Integer sessionCount;  // Số lần sạc
}

