package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.IncidentSeverity;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentCreationRequest {
    String stationId;
    String chargingPointId;
    String description;
    IncidentSeverity severity;
}
