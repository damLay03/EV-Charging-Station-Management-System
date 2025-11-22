package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.IncidentSeverity;
import com.swp.evchargingstation.enums.IncidentStatus;
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
    String chargingPointName;
    LocalDateTime reportedAt;
    String description;
    IncidentSeverity severity;
    IncidentStatus status;
    String assignedStaffName;
    LocalDateTime resolvedAt;
    String imageUrl;
}
