package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverResponse {
    // Fields từ User
    String userId;
    String email;
    String phone;
    LocalDate dateOfBirth;
    boolean gender;
    String firstName;
    String lastName;
    String fullName;
    Role role;

    // Fields từ Driver entity
    String address;
    LocalDateTime joinDate;
}

