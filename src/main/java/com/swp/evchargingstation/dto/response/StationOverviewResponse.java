package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.StationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationOverviewResponse {
    String stationId;
    String name;
    StationStatus status;
    boolean active; // true if status == OPERATIONAL
}

