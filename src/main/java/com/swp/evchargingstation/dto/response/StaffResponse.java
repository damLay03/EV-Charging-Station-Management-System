package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Gender;
import com.swp.evchargingstation.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffResponse {
    // Fields từ User
    String userId;
    String email;
    String phone;
    LocalDate dateOfBirth;
    Gender gender;
    String firstName;
    String lastName;
    String fullName;
    Role role;

    // Fields từ Staff entity
    String employeeNo;
    String position;

    // Thông tin trạm được gán (managedStation)
    String managedStationId;
    String managedStationName;
}

