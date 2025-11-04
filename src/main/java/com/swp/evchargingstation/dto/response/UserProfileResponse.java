package com.swp.evchargingstation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp.evchargingstation.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    Role role;
    DriverResponse driverProfile;
    StaffProfileResponse staffProfile;
    AdminProfileResponse adminProfile;
}