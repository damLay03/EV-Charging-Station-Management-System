package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CashPaymentRequest {
    @NotBlank(message = "Session ID is required")
    String sessionId;
}

