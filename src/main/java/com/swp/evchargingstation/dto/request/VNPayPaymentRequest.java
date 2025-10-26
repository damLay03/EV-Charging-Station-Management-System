package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class VNPayPaymentRequest {
    @NotBlank(message = "Session ID is required")
    String sessionId; // ID của charging session cần thanh toán

    String bankCode; // Optional: NCB, VNPAYQR, VNBANK, INTCARD, etc.
}
