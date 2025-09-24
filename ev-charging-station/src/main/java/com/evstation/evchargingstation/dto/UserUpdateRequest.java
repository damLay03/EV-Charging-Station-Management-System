package com.evstation.evchargingstation.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    String email;
    String password;
    String phone;
    String fullName;
}
