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

                Optional<Booking> relatedBookingOpt = bookingRepository.findByUserIdAndChargingPointIdAndBookingStatus(
                        session.getDriver().getUserId(),
                        session.getChargingPoint().getPointId(),
                        BookingStatus.IN_PROGRESS
                );

                if (relatedBookingOpt.isPresent()) {
                    Booking booking = relatedBookingOpt.get();
                    String userId = session.getDriver().getUserId();

                    double deposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : 0.0;

                    if (cost > deposit) {
                        double amountToDebit = cost - deposit;
                        walletService.debit(
                                userId,
                                amountToDebit,
                                TransactionType.CHARGING_PAYMENT,
                                String.format("Trừ tiền tự động cho phiên sạc %s (chi phí %.0f - đặt cọc %.0f)",
                                        session.getSessionId(), cost, deposit),
                                booking.getId(),
                                session.getSessionId()
                        );
                        log.info("[Settlement] Debited {} VND from wallet for session {} (deposit applied)", amountToDebit, session.getSessionId());
                    } else {
                        double refund = deposit - cost;
                        if (refund > 0) {
                            walletService.credit(
                                    userId,
                                    refund,
                                    TransactionType.BOOKING_DEPOSIT_REFUND,
                                    String.format("Hoàn tiền đặt cọc %.0f cho đơn đặt chỗ #%d (phiên sạc %s)",
                                            refund, booking.getId(), session.getSessionId()),
                                    null,
                                    null,
                                    booking.getId(),
                                    session.getSessionId()
                            );
                            log.info("[Settlement] Refunded {} VND deposit for booking #{} (session {})", refund, booking.getId(), session.getSessionId());
                        }
                    }

                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaymentMethod(Payment.PaymentMethod.WALLET);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    booking.setBookingStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                } else {
                    // No booking: full wallet debit
                    String userId = session.getDriver().getUserId();
                    walletService.debit(
                            userId,
                            (double) cost,
                            TransactionType.CHARGING_PAYMENT,
                            String.format("Thanh toán tự động từ ví cho phiên sạc %s", session.getSessionId()),
                            null,
                            session.getSessionId()
                    );

                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaymentMethod(Payment.PaymentMethod.WALLET);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    log.info("[Settlement] Auto-paid {} VND from wallet for session {} (no booking)", cost, session.getSessionId());
                }

                paymentRepository.flush(); // single flush

                // Gửi email thông báo thanh toán thành công
                try {
                    if (session.getDriver() != null && session.getDriver().getUser() != null) {
                        emailService.sendChargingPaymentSuccessEmail(
                            session.getDriver().getUser(),
                            session,
                            cost
                        );
                    }
                } catch (Exception emailEx) {
                    log.warn("[Settlement] Failed to send payment email for session {}: {}",
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
