package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleAssignmentRequest {
    @NotNull(message = "Role không được để trống")
    Role role;
}

