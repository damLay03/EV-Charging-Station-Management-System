package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.BillingType;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
}

