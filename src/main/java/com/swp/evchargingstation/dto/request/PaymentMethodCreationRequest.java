package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.PaymentMethodType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentMethodCreationRequest {
    @NotNull(message = "METHOD_TYPE_REQUIRED")
    PaymentMethodType methodType;

    String provider; // Ví dụ: "Visa", "MoMo", "ZaloPay"

    String token; // Số thẻ/tài khoản (nên được mã hóa)
}

