package com.swp.evchargingstation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletDashboardResponse {
    private Double currentBalance;
    private WalletStatistics statistics;

    @Data
    @Builder
    public static class WalletStatistics {
        private Double monthlySpending;      // Chi tiêu tháng này
        private Double monthlyTopUp;         // Nạp tháng này
        private Integer transactionCount;    // Số giao dịch tháng này
    }
}

