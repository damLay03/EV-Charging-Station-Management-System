package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.Booking;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.enums.BookingStatus;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.repository.BookingRepository;
import com.swp.evchargingstation.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSettlementService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final WalletService walletService;
    private final EmailService emailService;

    /**
     * Settle payment for a COMPLETED charging session using wallet only.
     * Runs in a separate transaction to avoid rolling back the outer stop flow.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settlePaymentForCompletedSession(ChargingSession session, float cost) {
        int maxRetries = 3;
        int attempt = 0;
        long backoffMs = 200; // simple backoff
        while (true) {
            try {
                // Idempotency: skip if already COMPLETED
                Optional<Payment> existingOpt = paymentRepository.findByChargingSession(session);
                if (existingOpt.isPresent() && existingOpt.get().getStatus() == PaymentStatus.COMPLETED) {
                    log.info("[Settlement] Payment already COMPLETED for session {} - skip", session.getSessionId());
                    return;
                }

                // Create payment row if missing
                if (existingOpt.isEmpty()) {
                    Payment newPayment = Payment.builder()
                            .payer(session.getDriver())
                            .amount(cost)
                            .status(PaymentStatus.UNPAID)
                            .chargingSession(session)
                            .paymentMethod(Payment.PaymentMethod.WALLET)
                            .createdAt(LocalDateTime.now())
                            .build();
                    paymentRepository.save(newPayment); // defer flush
                    log.info("[Settlement] Created UNPAID payment for session {} amount {}", session.getSessionId(), cost);
                }

                Payment payment = paymentRepository.findByChargingSession(session).orElseThrow();

                // FIX: Tìm booking qua session relationship hoặc tìm theo user + point với cả IN_PROGRESS và COMPLETED
                // Vì booking có thể đã được mark COMPLETED trước khi settlement chạy
                Optional<Booking> relatedBookingOpt = Optional.empty();

                // Try 1: Get booking from session if available
                if (session.getBooking() != null) {
                    relatedBookingOpt = Optional.of(session.getBooking());
                    log.debug("[Settlement] Found booking #{} from session relationship", session.getBooking().getId());
                }

                // Try 2: Find by user + point with IN_PROGRESS status
                if (relatedBookingOpt.isEmpty()) {
                    relatedBookingOpt = bookingRepository.findByUserIdAndChargingPointIdAndBookingStatus(
                            session.getDriver().getUserId(),
                            session.getChargingPoint().getPointId(),
                            BookingStatus.IN_PROGRESS
                    );
                    if (relatedBookingOpt.isPresent()) {
                        log.debug("[Settlement] Found booking #{} with IN_PROGRESS status", relatedBookingOpt.get().getId());
                    }
                }

                // Try 3: Find recently COMPLETED booking (trong vòng 5 phút)
                if (relatedBookingOpt.isEmpty()) {
                    relatedBookingOpt = bookingRepository.findByUserIdAndChargingPointIdAndBookingStatus(
                            session.getDriver().getUserId(),
                            session.getChargingPoint().getPointId(),
                            BookingStatus.COMPLETED
                    );
                    if (relatedBookingOpt.isPresent()) {
                        log.debug("[Settlement] Found booking #{} with COMPLETED status", relatedBookingOpt.get().getId());
                    }
                }

                if (relatedBookingOpt.isPresent()) {
                    Booking booking = relatedBookingOpt.get();
                    String userId = session.getDriver().getUserId();

                    double deposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : 0.0;
                    double currentBalance = walletService.getBalance(userId);

                    if (cost > deposit) {
                        double amountToDebit = cost - deposit;

                        // Check sufficient funds
                        if (currentBalance >= amountToDebit) {
                            try {
                                walletService.debit(
                                        userId,
                                        amountToDebit,
                                        TransactionType.CHARGING_PAYMENT,
                                        String.format("Auto-net debit for charging session %s (cost %.0f - deposit %.0f)",
                                                session.getSessionId(), cost, deposit),
                                        booking.getId(),
                                        session.getSessionId()
                                );
                                payment.setStatus(PaymentStatus.COMPLETED);
                                payment.setPaidAt(LocalDateTime.now());
                                log.info("[Settlement] Debited {} VND from wallet for session {} (deposit applied)", amountToDebit, session.getSessionId());
                            } catch (Exception e) {
                                // Insufficient funds → Mark as UNPAID (debt)
                                payment.setStatus(PaymentStatus.UNPAID);
                                log.warn("[Settlement] Insufficient funds for session {}. Balance: {}, Required: {}. Marked as UNPAID (debt)",
                                        session.getSessionId(), currentBalance, amountToDebit);
                            }
                        } else {
                            // Insufficient funds → Mark as UNPAID (debt)
                            payment.setStatus(PaymentStatus.UNPAID);
                            log.warn("[Settlement] Insufficient funds for session {}. Balance: {}, Required: {}. Marked as UNPAID (debt)",
                                    session.getSessionId(), currentBalance, amountToDebit);
                        }
                    } else {
                        // Cost <= deposit → Always COMPLETED and refund if needed
                        log.info("💰 [Settlement] Cost <= Deposit - Payment COMPLETED, checking refund...");

                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaidAt(LocalDateTime.now());

                        double refund = deposit - cost;
                        log.info("[Settlement] Refund calculation: Deposit {} - Cost {} = {} VND",
                                deposit, cost, refund);

                        if (refund > 0) {
                            log.info("[Settlement] Refunding {} VND to user {}", refund, userId);

                            walletService.credit(
                                    userId,
                                    refund,
                                    TransactionType.BOOKING_DEPOSIT_REFUND,
                                    String.format("Deposit refund %.0f VND for booking #%d (session %s)",
                                            refund, booking.getId(), session.getSessionId()),
                                    null,
                                    null,
                                    booking.getId(),
                                    session.getSessionId()
                            );
                            log.info("[Settlement] Successfully refunded {} VND deposit for booking #{} (session {})",
                                    refund, booking.getId(), session.getSessionId());
                        } else {
                            log.info("[Settlement] No refund needed - cost equals deposit exactly");
                        }
                    }

                    payment.setPaymentMethod(Payment.PaymentMethod.WALLET);
                    paymentRepository.save(payment);// Update booking status to COMPLETED if not already
                    if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
                        booking.setBookingStatus(BookingStatus.COMPLETED);
                        bookingRepository.save(booking);
                        log.info("[Settlement] Updated booking #{} status to COMPLETED", booking.getId());
                    } else {
                        log.debug("[Settlement] Booking #{} already COMPLETED", booking.getId());
                    }
                } else {
                    // No booking: full wallet debit
                    String userId = session.getDriver().getUserId();
                    double currentBalance = walletService.getBalance(userId);

                    if (currentBalance >= cost) {
                        try {
                            walletService.debit(
                                    userId,
                                    (double) cost,
                                    TransactionType.CHARGING_PAYMENT,
                                    String.format("Auto wallet payment for session %s", session.getSessionId()),
                                    null,
                                    session.getSessionId()
                            );
                            payment.setStatus(PaymentStatus.COMPLETED);
                            payment.setPaidAt(LocalDateTime.now());
                            log.info("[Settlement] Auto-paid {} VND from wallet for session {} (no booking)", cost, session.getSessionId());
                        } catch (Exception e) {
                            // Insufficient funds → Mark as UNPAID
                            payment.setStatus(PaymentStatus.UNPAID);
                            log.warn("[Settlement] Insufficient funds for session {} (no booking). Balance: {}, Required: {}. Marked as UNPAID (debt)",
                                    session.getSessionId(), currentBalance, cost);
                        }
                    } else {
                        // Insufficient funds → Mark as UNPAID
                        payment.setStatus(PaymentStatus.UNPAID);
                        log.warn("[Settlement] Insufficient funds for session {} (no booking). Balance: {}, Required: {}. Marked as UNPAID (debt)",
                                session.getSessionId(), currentBalance, cost);
                    }

                    payment.setPaymentMethod(Payment.PaymentMethod.WALLET);
                    paymentRepository.save(payment);
                }

                paymentRepository.flush(); // single flush

                // Gửi email thông báo
                try {
                    if (session.getDriver() != null && session.getDriver().getUser() != null) {
                        if (payment.getStatus() == PaymentStatus.COMPLETED) {
                            // Payment thành công
                            emailService.sendChargingPaymentSuccessEmail(
                                session.getDriver().getUser(),
                                session,
                                cost
                            );
                        } else {
                            // Payment UNPAID (insufficient funds) -
                            log.info("[Settlement] Payment UNPAID for session {} - user needs to top-up wallet",
                                    session.getSessionId());
                        }
                    }
                } catch (Exception emailEx) {
                    log.warn("[Settlement] Failed to send email for session {}: {}",
                            session.getSessionId(), emailEx.getMessage());
                }

                return; // success
            } catch (org.springframework.dao.PessimisticLockingFailureException | org.hibernate.PessimisticLockException ex) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("[Settlement] Failed after {} attempts for session {} due to lock timeout: {}", attempt, session.getSessionId(), ex.getMessage());
                    throw ex;
                }
                log.warn("[Settlement] Lock timeout attempt {} for session {}. Retrying after {}ms", attempt, session.getSessionId(), backoffMs);
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                backoffMs *= 2; // exponential backoff
            }
        }
    }
}
