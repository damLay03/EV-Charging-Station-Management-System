package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.Gender;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    // NOTE: Tất cả field đều OPTIONAL cho partial update.
    // Null => không đổi. Nếu FE muốn reset giá trị phải gửi giá trị hợp lệ (không hỗ trợ set rỗng hoàn toàn ở đây).
    private String phone;      // optional
    private LocalDate dateOfBirth; // optional
    private Gender gender;    // optional (MALE, FEMALE, OTHER)
    private String firstName;  // optional
    private String lastName;   // optional
    private String address;    // optional - cho driver cập nhật địa chỉ
}