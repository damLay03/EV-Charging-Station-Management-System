package com.swp.evchargingstation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfileResponse {
    private String staffId;     // userId
    private String email;
    private String fullName;
    private String phone;
    private String employeeNo;
    private String position;
    private String stationId;    // có thể null nếu chưa gán
    private String stationName;  // có thể null nếu chưa gán
    private String stationAddress; // có thể null nếu chưa gán
}
