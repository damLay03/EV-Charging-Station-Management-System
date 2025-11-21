package com.swp.evchargingstation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {
    private String walletId; // Changed from Long to String (UUID)
    private String userId;
    private Double balance;
    private String updatedAt;
}

