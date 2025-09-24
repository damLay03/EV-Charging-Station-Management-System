package com.evstation.evchargingstation.dto;

import com.evstation.evchargingstation.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class UserCreationRequest {
    String email;
    String password;
    String phone;
    String fullName;
    User.Role role;
}
