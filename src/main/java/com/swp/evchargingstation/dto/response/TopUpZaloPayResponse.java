package com.swp.evchargingstation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpZaloPayResponse {
    private String orderUrl;
    private String appTransId;
    private Long transactionId;
    private String message;
}

