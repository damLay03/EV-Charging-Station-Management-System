package com.swp.evchargingstation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCallbackResponse {
    private String code;
    private String message;
    private String sessionId;
    private String transactionId; // Mã giao dịch VNPay
    private Long amount;
    private String paymentStatus; // SUCCESS, FAILED
    private String bankCode;
    private String cardType;
    private String payDate;
}

