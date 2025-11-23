package com.swp.evchargingstation.event.wallet;

import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi wallet được credit (nạp tiền).
 *
 * Use cases:
 * - Gửi email thông báo top-up thành công
 * - Gửi email cảnh báo nếu balance thấp
 * - Track analytics cho wallet transactions
 *
 * Listeners:
 * - TopUpEmailListener: Gửi email khi top-up (ASYNC)
 * - LowBalanceWarningListener: Gửi warning nếu balance < threshold (ASYNC)
 * - AnalyticsListener: Track top-up metrics (ASYNC)
 */
@Getter
public class WalletCreditedEvent extends ApplicationEvent {

    private final Wallet wallet;
    private final String userId;
    private final Double amount;
    private final TransactionType transactionType;
    private final Double newBalance;
    private final String description;

    public WalletCreditedEvent(Object source, Wallet wallet, Double amount,
                               TransactionType type, String description) {
        super(source);
        this.wallet = wallet;
        this.userId = wallet.getUser().getUserId();
        this.amount = amount;
        this.transactionType = type;
        this.newBalance = wallet.getBalance();
        this.description = description;
    }

    /**
     * Check if this is a top-up transaction (cash or ZaloPay)
     */
    public boolean isTopUp() {
        return transactionType == TransactionType.TOPUP_CASH ||
               transactionType == TransactionType.TOPUP_ZALOPAY;
    }

    /**
     * Check if this is a refund transaction
     */
    public boolean isRefund() {
        return transactionType == TransactionType.BOOKING_DEPOSIT_REFUND;
    }
}

