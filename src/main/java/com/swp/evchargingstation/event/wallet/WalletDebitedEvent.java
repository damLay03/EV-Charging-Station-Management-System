package com.swp.evchargingstation.event.wallet;

import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi wallet được debit (trừ tiền).
 *
 * Use cases:
 * - Gửi email thông báo payment thành công
 * - Gửi warning nếu balance thấp sau khi debit
 * - Track spending analytics
 *
 * Listeners:
 * - PaymentEmailListener: Gửi email payment confirmation (ASYNC)
 * - LowBalanceWarningListener: Gửi warning nếu balance < threshold (ASYNC)
 * - SpendingAnalyticsListener: Track spending patterns (ASYNC)
 */
@Getter
public class WalletDebitedEvent extends ApplicationEvent {

    private final Wallet wallet;
    private final String userId;
    private final Double amount;
    private final TransactionType transactionType;
    private final Double newBalance;
    private final String description;

    public WalletDebitedEvent(Object source, Wallet wallet, Double amount,
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
     * Check if this is a booking deposit
     */
    public boolean isBookingDeposit() {
        return transactionType == TransactionType.BOOKING_DEPOSIT;
    }

    /**
     * Check if this is a charging payment
     */
    public boolean isChargingPayment() {
        return transactionType == TransactionType.CHARGING_PAYMENT;
    }

    /**
     * Check if balance is low (< 100,000 VND)
     */
    public boolean isLowBalance() {
        return newBalance < 100000;
    }
}

