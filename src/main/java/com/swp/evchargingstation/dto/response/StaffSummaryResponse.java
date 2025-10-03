package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffSummaryResponse {
    String staffId;      // = userId
    String employeeNo;
    String position;
    String email;
    String fullName;     // firstName + lastName từ User
    String stationId;    // null nếu chưa được gán
    String stationName;  // null nếu chưa được gán
}

