package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionCreationRequest {
    @NotBlank(message = "PLAN_ID_REQUIRED")
    String planId;

    String paymentMethodId; // ID của payment method để thanh toán (nếu có phí)

    boolean autoRenew; // Tự động gia hạn hay không
}

