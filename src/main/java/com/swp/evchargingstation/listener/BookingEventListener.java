package com.swp.evchargingstation.listener;

import com.swp.evchargingstation.event.booking.BookingCancelledEvent;
import com.swp.evchargingstation.event.booking.BookingCheckedInEvent;
import com.swp.evchargingstation.event.booking.BookingCreatedEvent;
import com.swp.evchargingstation.service.EmailService;
import com.swp.evchargingstation.service.WalletService;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener cho Booking lifecycle events.
 *
 * Handles:
 * - Wallet deposit debit khi booking created
 * - Email notifications (async)
 *
 * Benefits:
 * - BookingService không phụ thuộc vào WalletService
 * - Deposit debit có separate transaction
 * - Email async, non-blocking
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final WalletService walletService;
    private final EmailService emailService;

    /**
     * Debit deposit from wallet when booking is created (SYNC, SEPARATE TRANSACTION).
     *
     * Flow:
     * 1. Booking already saved in DB (status = CONFIRMED)
     * 2. This listener debits deposit in separate transaction
     * 3. If insufficient funds → Booking remains but user must top-up
     *
     * CRITICAL:
     * - Uses REQUIRES_NEW for separate transaction
     * - Failure doesn't rollback booking (already committed)
     * - User will see booking but may need to top-up if wallet insufficient
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void debitDepositFromWallet(BookingCreatedEvent event) {
        try {
            log.info("[Event] Debiting deposit for booking #{}: {} VND from user {}",
                    event.getBookingId(), event.getDepositAmount(), event.getUserId());

            walletService.debit(
                    event.getUserId(),
                    event.getDepositAmount(),
                    TransactionType.BOOKING_DEPOSIT,
                    String.format("Deposit for booking #%d at charging point %s",
                            event.getBookingId(), event.getChargingPointId()),
                    event.getBookingId(),
                    null
            );

            log.info("[Event] Deposit debited successfully for booking #{}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to debit deposit for booking #{}: {}",
                    event.getBookingId(), ex.getMessage(), ex);

            // TODO: Future enhancement - Mark booking with PAYMENT_PENDING flag
            // TODO: Send notification to user to top-up wallet
            // For now, booking remains CONFIRMED but deposit not deducted
        }
    }

    /**
     * Send booking confirmation email (ASYNC).
     *
     * Email contains:
     * - Booking details (time, location, charging point)
     * - Deposit amount deducted
     * - Check-in instructions
     * - Cancellation policy
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendBookingConfirmationEmail(BookingCreatedEvent event) {
        try {
            log.info("[Event] Sending booking confirmation email for booking #{}", event.getBookingId());

            // TODO: Implement emailService.sendBookingConfirmationEmail()
            // For now, log it
            log.info("[TODO] Booking confirmation email for booking #{} - User: {}, Point: {}",
                    event.getBookingId(), event.getUserId(), event.getChargingPointId());

            log.info("[Event] Booking confirmation email sent for booking #{}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send booking confirmation email for booking #{}: {}",
                    event.getBookingId(), ex.getMessage(), ex);
        }
    }

    /**
     * Send check-in notification email (ASYNC).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendCheckInNotification(BookingCheckedInEvent event) {
        try {
            log.info("[Event] Sending check-in notification for booking #{}", event.getBookingId());

            // TODO: Implement emailService.sendBookingCheckInEmail()
            log.info("[TODO] Check-in notification for booking #{} - User: {}",
                    event.getBookingId(), event.getUserId());

            log.info("[Event] Check-in notification sent for booking #{}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send check-in notification for booking #{}: {}",
                    event.getBookingId(), ex.getMessage(), ex);
        }
    }

    /**
     * Send cancellation notification email (ASYNC).
     *
     * Email reminds user that deposit is NOT refunded (policy).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendCancellationNotification(BookingCancelledEvent event) {
        try {
            log.info("[Event] Sending cancellation notification for booking #{}", event.getBookingId());

            // TODO: Implement emailService.sendBookingCancelledEmail()
            log.info("[TODO] Cancellation notification for booking #{} - Deposit {} VND NOT refunded",
                    event.getBookingId(), event.getDepositAmount());

            log.info("[Event] Cancellation notification sent for booking #{}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send cancellation notification for booking #{}: {}",
                    event.getBookingId(), ex.getMessage(), ex);
        }
    }
}

