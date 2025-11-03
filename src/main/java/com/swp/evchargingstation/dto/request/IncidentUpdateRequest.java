package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.IncidentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentUpdateRequest {
    IncidentStatus status; // WAITING, WORKING, DONE
    String description; // Cập nhật mô tả
}

