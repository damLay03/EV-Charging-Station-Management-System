package com.swp.evchargingstation.listener;

import com.swp.evchargingstation.event.session.ChargingSessionCompletedEvent;
import com.swp.evchargingstation.event.session.ChargingSessionStartedEvent;
import com.swp.evchargingstation.service.EmailService;
import com.swp.evchargingstation.service.PaymentSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener cho ChargingSession lifecycle events.
 *
 * Tách biệt side effects (email, payment) ra khỏi core business logic:
 * - Email notifications: Async, non-blocking
 * - Payment settlement: Separate transaction, doesn't rollback session if fails
 *
 * Benefits:
 * - Shorter transaction duration in ChargingSimulatorService
 * - Isolated failures (email fail không affect payment, payment fail không rollback session)
 * - Easy to add new listeners without modifying service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChargingSessionEventListener {

    private final EmailService emailService;
    private final PaymentSettlementService paymentSettlementService;

    /**
     * Send email notification when session starts (ASYNC).
     *
     * @TransactionalEventListener(AFTER_COMMIT): Chỉ chạy sau khi session đã save thành công
     * @Async: Chạy trong background thread, không block main flow
     *
     * If email fails: Logged nhưng không affect session
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendStartNotification(ChargingSessionStartedEvent event) {
        try {
            log.info("[Event] Sending start email for session: {}", event.getSessionId());
            emailService.sendChargingStartEmail(event.getSession());
            log.info("[Event] Start email sent successfully for session: {}", event.getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send start email for session {}: {}",
                    event.getSessionId(), ex.getMessage(), ex);
            // Don't rethrow - email failure should not affect main flow
        }
    }

    /**
     * Send email notification when session completes (ASYNC).
     *
     * Email chứa:
     * - Summary: Duration, energy consumed, cost
     * - Payment status
     * - Receipt/invoice
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendCompletionNotification(ChargingSessionCompletedEvent event) {
        try {
            log.info("[Event] Sending completion email for session: {}", event.getSessionId());
            emailService.sendChargingCompleteEmail(event.getSession());
            log.info("[Event] Completion email sent successfully for session: {}", event.getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send completion email for session {}: {}",
                    event.getSessionId(), ex.getMessage(), ex);
        }
    }

    /**
     * Settle payment when session completes (SYNC, SEPARATE TRANSACTION).
     *
     * IMPORTANT:
     * - This MUST be synchronous để ensure payment được process
     * - Uses REQUIRES_NEW để có separate transaction
     * - Nếu payment fails, session vẫn COMPLETED (không rollback)
     * - User sẽ thấy UNPAID status trong dashboard và phải top-up
     *
     * Flow:
     * 1. Check if booking exists → Apply deposit
     * 2. Debit from wallet (or mark UNPAID if insufficient funds)
     * 3. Refund deposit if cost < deposit
     * 4. Send payment email (handled by PaymentSettlementService)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settlePayment(ChargingSessionCompletedEvent event) {
        try {
            log.info("[Event] Settling payment for session: {} (cost: {} VND)",
                    event.getSessionId(), event.getTotalCost());

            paymentSettlementService.settlePaymentForCompletedSession(
                    event.getSession(),
                    event.getTotalCost()
            );

            log.info("[Event] Payment settled successfully for session: {}", event.getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to settle payment for session {}: {}",
                    event.getSessionId(), ex.getMessage(), ex);
            // Payment failure is logged but doesn't affect session completion
            // User will see UNPAID status in their dashboard
            // Admin can manually resolve or user can top-up and retry
        }
    }
}

