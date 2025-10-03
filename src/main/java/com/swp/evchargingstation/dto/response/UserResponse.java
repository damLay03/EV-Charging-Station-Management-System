package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class UserResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId;

    @Column(nullable = false, unique = true)
    String email;
    String password;
    String phone;
    LocalDate dateOfBirth;
    boolean gender;
    String firstName;
    String lastName;
    String fullName;
    Role role;
}
