package com.swp.evchargingstation.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse {
    String fullName;
    String email;
    String phone;
    LocalDate joinDate; // Ngày tham gia (sẽ set null nếu chưa có)
    String planName; // Gói dịch vụ (Premium, Basic, VIP)
    Integer sessionCount; // Số phiên sạc
    Double totalSpent; // Tổng chi tiêu
    String status; // Trạng thái (Hoạt động, etc.)
    Boolean isActive; // Chi tiết trạng thái
}
