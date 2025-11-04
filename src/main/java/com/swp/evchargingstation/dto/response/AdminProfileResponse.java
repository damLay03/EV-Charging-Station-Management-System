package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminProfileResponse {
    String adminId;     // userId
    String email;
    String fullName;
    String firstName;
    String lastName;
    String phone;
    LocalDate dateOfBirth;
    Gender gender;
    String department;  // có thể null
}