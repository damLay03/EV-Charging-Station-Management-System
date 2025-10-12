package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.PaymentMethodType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentMethodResponse {
    String pmId;
    PaymentMethodType methodType;
    String provider;
    String maskedToken; // Chỉ hiển thị 4 số cuối
}

