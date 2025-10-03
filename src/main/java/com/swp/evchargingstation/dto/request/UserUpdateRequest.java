package com.swp.evchargingstation.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    // NOTE: Tất cả field đều OPTIONAL cho partial update.
    // Null => không đổi. Nếu FE muốn reset giá trị phải gửi giá trị hợp lệ (không hỗ trợ set rỗng hoàn toàn ở đây).
    private String phone;      // optional
    private LocalDate dateOfBirth; // optional
    private Boolean gender;    // optional (true = male, false = female)
    private String firstName;  // optional
    private String lastName;   // optional
}