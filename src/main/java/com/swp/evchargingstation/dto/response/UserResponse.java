package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class UserResponse {
    String userId;
    String email;
    String password;
    String phone;
    String fullName;
    Role role;
}
