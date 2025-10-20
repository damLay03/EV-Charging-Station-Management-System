package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentResponse {
    String incidentId;
    String reporterName;
    String stationName;
    String chargingPointId;
    LocalDateTime reportedAt;
    String description;
    String severity;
    String status;
    String assignedStaffName;
    LocalDateTime resolvedAt;
}
