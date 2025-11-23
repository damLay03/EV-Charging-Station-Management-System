package com.swp.evchargingstation.listener;

import com.swp.evchargingstation.event.wallet.WalletCreditedEvent;
import com.swp.evchargingstation.event.wallet.WalletDebitedEvent;
import com.swp.evchargingstation.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener cho Wallet transaction events.
 *
 * Handles:
 * - Email notifications cho top-up
 * - Email notifications cho payments
 * - Low balance warnings
 *
 * Benefits:
 * - WalletService không phụ thuộc vào EmailService
 * - Email async, non-blocking
 * - Dễ thêm listeners mới (analytics, alerts, etc.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final EmailService emailService;

    private static final double LOW_BALANCE_THRESHOLD = 100000.0; // 100,000 VND

    /**
     * Send email notification when wallet is credited (ASYNC).
     *
     * Scenarios:
     * - Top-up cash: "Bạn đã nạp tiền thành công"
     * - Top-up ZaloPay: "Nạp tiền qua ZaloPay thành công"
     * - Refund: "Hoàn tiền cọc booking"
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendCreditNotification(WalletCreditedEvent event) {
        try {
            if (event.isTopUp()) {
                log.info("[Event] Sending top-up email for user: {} - Amount: {} VND",
                        event.getUserId(), event.getAmount());

                emailService.sendWalletTopUpSuccessEmail(
                        event.getWallet().getUser(),
                        event.getAmount(),
                        event.getNewBalance()
                );

                log.info("[Event] Top-up email sent successfully for user: {}", event.getUserId());
            } else if (event.isRefund()) {
                log.info("[Event] Sending refund notification for user: {} - Amount: {} VND",
                        event.getUserId(), event.getAmount());

                // TODO: Implement emailService.sendRefundEmail()
                log.info("[TODO] Refund email for user: {} - {} VND",
                        event.getUserId(), event.getAmount());

                log.info("[Event] Refund notification logged for user: {}", event.getUserId());
            } else {
                // Other credit types (if any)
                log.info("[Event] Wallet credited for user: {} - Type: {}, Amount: {} VND",
                        event.getUserId(), event.getTransactionType(), event.getAmount());
            }
        } catch (Exception ex) {
            log.error("[Event] Failed to send credit notification for user {}: {}",
                    event.getUserId(), ex.getMessage(), ex);
        }
    }

    /**
     * Send email notification when wallet is debited (ASYNC).
     *
     * Scenarios:
     * - Booking deposit: "Đã trừ 50,000 VND tiền cọc"
     * - Charging payment: "Thanh toán phí sạc thành công"
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendDebitNotification(WalletDebitedEvent event) {
        try {
            if (event.isBookingDeposit()) {
                log.info("[Event] Sending booking deposit debit email for user: {} - Amount: {} VND",
                        event.getUserId(), event.getAmount());

                // TODO: Implement emailService.sendBookingDepositDebitEmail()
                log.info("[TODO] Booking deposit email for user: {} - {} VND deducted",
                        event.getUserId(), event.getAmount());

            } else if (event.isChargingPayment()) {
                log.info("[Event] Sending charging payment email for user: {} - Amount: {} VND",
                        event.getUserId(), event.getAmount());

                // Payment email already sent by ChargingSessionEventListener
                // This is just for logging
                log.debug("Payment email already sent by ChargingSessionEventListener");

            } else {
                // Other debit types
                log.info("[Event] Wallet debited for user: {} - Type: {}, Amount: {} VND",
                        event.getUserId(), event.getTransactionType(), event.getAmount());
            }

            log.info("[Event] Debit notification processed for user: {}", event.getUserId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send debit notification for user {}: {}",
                    event.getUserId(), ex.getMessage(), ex);
        }
    }

    /**
     * Send low balance warning when wallet balance is low after credit (ASYNC).
     *
     * Triggers when balance < 100,000 VND after any credit transaction.
     * Helps user know they should top-up soon.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void checkLowBalanceAfterCredit(WalletCreditedEvent event) {
        try {
            if (event.getNewBalance() < LOW_BALANCE_THRESHOLD) {
                log.warn("[Event] Low balance detected for user: {} - Balance: {} VND (threshold: {})",
                        event.getUserId(), event.getNewBalance(), LOW_BALANCE_THRESHOLD);

                // TODO: Implement emailService.sendLowBalanceWarning()
                log.info("[TODO] Low balance warning for user: {} - Balance: {} VND",
                        event.getUserId(), event.getNewBalance());
            }
        } catch (Exception ex) {
            log.error("[Event] Failed to check low balance for user {}: {}",
                    event.getUserId(), ex.getMessage(), ex);
        }
    }

    /**
     * Send low balance warning when wallet balance is low after debit (ASYNC).
     *
     * Triggers when balance < 100,000 VND after any debit transaction.
     * Critical: User should top-up to avoid failed transactions.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void checkLowBalanceAfterDebit(WalletDebitedEvent event) {
        try {
            if (event.isLowBalance()) {
                log.warn("[Event] Low balance detected for user: {} - Balance: {} VND (threshold: {})",
                        event.getUserId(), event.getNewBalance(), LOW_BALANCE_THRESHOLD);

                // TODO: Implement emailService.sendLowBalanceWarning()
                log.info("[TODO] Low balance warning for user: {} - Balance: {} VND",
                        event.getUserId(), event.getNewBalance());

                log.info("[Event] Low balance warning logged for user: {}", event.getUserId());
            }
        } catch (Exception ex) {
            log.error("[Event] Failed to check low balance for user {}: {}",
                    event.getUserId(), ex.getMessage(), ex);
        }
    }
}

