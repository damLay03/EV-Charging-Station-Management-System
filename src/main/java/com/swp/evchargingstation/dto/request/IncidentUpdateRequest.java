package com.swp.evchargingstation.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentUpdateRequest {
    String status; // REPORTED, IN_PROGRESS, RESOLVED, CLOSED
    String resolution; // Giải pháp đã áp dụng
}

