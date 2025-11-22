package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.BillingType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanResponse {
    String planId;
    String name;
    BillingType billingType;
    float pricePerKwh;
    float pricePerMinute;
    float monthlyFee;
    String benefits;

    // Thông tin thời gian hết hạn (cho driver xem plan của mình)
    LocalDateTime planSubscriptionDate; // Ngày đăng ký
    LocalDateTime planExpiryDate;       // Ngày hết hạn
    Long daysUntilExpiry;               // Số ngày còn lại đến khi hết hạn
    Boolean autoRenewEnabled;           // Trạng thái tự động gia hạn
}

