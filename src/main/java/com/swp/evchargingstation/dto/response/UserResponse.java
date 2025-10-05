package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class UserResponse {
    String userId;
    String email;
    // Đã xóa field password vì lý do bảo mật - không nên trả password về client
    String phone;
    LocalDate dateOfBirth;
    boolean gender;
    String firstName;
    String lastName;
    String fullName;
    Role role;
}
