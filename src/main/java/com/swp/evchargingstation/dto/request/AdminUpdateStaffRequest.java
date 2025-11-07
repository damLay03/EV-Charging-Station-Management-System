package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.Gender;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminUpdateStaffRequest {
    // NOTE: Admin có thể update tất cả thông tin của staff (trừ email, password)
    // Tất cả field đều OPTIONAL cho partial update.
    private String phone;      // optional
    private LocalDate dateOfBirth; // optional
    private Gender gender;    // optional (MALE, FEMALE, OTHER)
    private String firstName;  // optional
    private String lastName;   // optional
    private String employeeNo; // optional - mã nhân viên
    private String position;   // optional - chức vụ
}

