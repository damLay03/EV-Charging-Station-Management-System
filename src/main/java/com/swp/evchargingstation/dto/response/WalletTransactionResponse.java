package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.TransactionStatus;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long id;
    private Double amount;
    private TransactionType transactionType;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String description;
    private String externalTransactionId;
    private String processedByStaffId;
    private String processedByStaffName;
    private Long relatedBookingId;
    private String relatedSessionId;
}

