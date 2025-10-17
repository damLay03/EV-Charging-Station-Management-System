package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.StationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationResponse {
    String stationId;
    String name;
    String address;
    String operatorName;
    String contactPhone;
    Double latitude;
    Double longitude;
    StationStatus status;
    boolean active; // convenience flag: true if status == OPERATIONAL
    String staffId; // ID của staff quản lý
    String staffName; // Tên đầy đủ của staff quản lý
}
