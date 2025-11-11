package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.BookingStatus;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChargingSimulatorService {

    ChargingSessionRepository chargingSessionRepository;
    VehicleRepository vehicleRepository;
    ChargingPointRepository chargingPointRepository;
    PlanRepository planRepository;
    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    WalletService walletService;
    EmailService emailService;

    // Phase 2: background simulation tick, runs every 1 seconds
    // Chạy mỗi 1 giây
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void simulateChargingTick() {
        List<ChargingSession> activeSessions = chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);

        if (!activeSessions.isEmpty()) {
            log.debug("Running charging simulation for {} active sessions", activeSessions.size());
        }

        for (ChargingSession session : activeSessions) {
            try {
                Vehicle vehicle = session.getVehicle();
                ChargingPoint point = session.getChargingPoint();
                if (vehicle == null || point == null) continue;

                float powerKw = point.getChargingPower().getPowerKw();
                float capacityKwh = vehicle.getBatteryCapacityKwh();
                int targetSoc = session.getTargetSocPercent() != null ? session.getTargetSocPercent() : 100;

                if (capacityKwh <= 0) {
                    log.warn("Vehicle {} has non-positive capacity. Skipping session {}", vehicle.getVehicleId(), session.getSessionId());
                    continue;
                }

                // Kiểm tra endSocPercent đã được khởi tạo chưa
                if (session.getEndSocPercent() == 0 && session.getStartSocPercent() > 0) {
                    session.setEndSocPercent(session.getStartSocPercent());
                }

                // Giả lập 1 giây một tick (vì fixedRate = 1000ms)
                // Tăng tốc độ sạc lên để test dễ hơn: mỗi tick = 1 phút thay vì 1 giây
                float timePerTickMinutes = 1.0f; // Mỗi tick = 1 phút
                float energyPerTick = powerKw * (timePerTickMinutes / 60.0f); // Năng lượng trong 1 phút
                float socAddedPerTick = (energyPerTick / capacityKwh) * 100.0f;

                int currentSoc = session.getEndSocPercent();
                float newSocFloat = currentSoc + socAddedPerTick;
                int newSocRounded = Math.round(newSocFloat);

                // Cập nhật thời gian: mỗi tick = 1 phút
                session.setDurationMin(session.getDurationMin() + timePerTickMinutes);
                session.setEnergyKwh(session.getEnergyKwh() + energyPerTick);

                if (newSocRounded >= targetSoc) {
                    // Đạt mục tiêu, dừng sạc
                    session.setEndSocPercent(targetSoc);
                    vehicle.setCurrentSocPercent(targetSoc);
                    stopSessionLogic(session, ChargingSessionStatus.COMPLETED);

                    // Send email after session stopped (outside transaction)
                    sendCompletionEmailAsync(session);
                } else {
                    session.setEndSocPercent(newSocRounded);
                    vehicle.setCurrentSocPercent(newSocRounded);
                    chargingSessionRepository.save(session);
                    vehicleRepository.save(vehicle);
                }
            } catch (Exception ex) {
                log.error("Error simulating session {}: {}", session.getSessionId(), ex.getMessage(), ex);
            }
        }
    }

    // Phase 3: stop logic (used by scheduler and user-triggered stop)
    @Transactional
    public void stopSessionLogic(ChargingSession session, ChargingSessionStatus finalStatus) {
        // Sanity checks
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            // Already stopped; no-op
            log.warn("Attempted to stop session {} which is already in status {}", session.getSessionId(), session.getStatus());
            return;
        }

        // Update session status and end time
        session.setStatus(finalStatus);
        session.setEndTime(LocalDateTime.now());

        // Calculate duration if not set
        if (session.getDurationMin() == 0 && session.getStartTime() != null) {
            long minutes = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
            session.setDurationMin((float) minutes);
        }

        // Update vehicle's final SOC
        Vehicle vehicle = session.getVehicle();
        if (vehicle != null && session.getEndSocPercent() > 0) {
            vehicle.setCurrentSocPercent(session.getEndSocPercent());
            vehicleRepository.save(vehicle);
            log.info("Updated vehicle {} SOC to {}%", vehicle.getVehicleId(), session.getEndSocPercent());
        }

        // Calculate cost - use driver's plan or fallback to "Linh hoạt"
        Driver driver = session.getDriver();
        Plan driverPlan = driver != null ? driver.getPlan() : null;

        Plan planToUse = driverPlan;
        if (planToUse == null) {
            // Fallback to "Linh hoạt" if driver has no plan
            planToUse = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
            log.info("Driver has no plan, using 'Linh hoạt' as fallback for session {}", session.getSessionId());
        } else {
            log.info("Using driver's plan '{}' for cost calculation of session {}", planToUse.getName(), session.getSessionId());
        }

        float cost = 0f;
        if (planToUse != null) {
            cost = (session.getEnergyKwh() * planToUse.getPricePerKwh()) + (session.getDurationMin() * planToUse.getPricePerMinute());
            log.info("Calculated cost for session {}: {} kWh * {} + {} min * {} = {}",
                session.getSessionId(),
                session.getEnergyKwh(),
                planToUse.getPricePerKwh(),
                session.getDurationMin(),
                planToUse.getPricePerMinute(),
                cost);
        } else {
            log.warn("No plan found, cost will be 0");
        }
        session.setCostTotal(cost);

        // Release charging point
        ChargingPoint point = session.getChargingPoint();
        if (point != null) {
            point.setStatus(ChargingPointStatus.AVAILABLE);
            point.setCurrentSession(null);
            chargingPointRepository.saveAndFlush(point);
            log.info("Released charging point {}", point.getPointId());
        }

        // Automatically create Payment record with UNPAID status when session is COMPLETED
        if (finalStatus == ChargingSessionStatus.COMPLETED) {
            // Check if payment already exists (avoid duplicate)
            boolean paymentExists = paymentRepository.findByChargingSession(session).isPresent();

            if (!paymentExists) {
                Payment payment = Payment.builder()
                        .payer(session.getDriver())
                        .amount(cost)
                        .status(PaymentStatus.UNPAID)
                        .chargingSession(session)
                        .paymentMethod(Payment.PaymentMethod.CASH) // Set default payment method
                        .createdAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(payment);
                log.info("Created UNPAID payment for completed session {} with amount {}", session.getSessionId(), cost);

                // Check if this session is related to a booking (IN_PROGRESS status)
                Optional<Booking> relatedBooking = bookingRepository.findByUserIdAndChargingPointIdAndBookingStatus(
                        session.getDriver().getUserId(),
                        session.getChargingPoint().getPointId(),
                        BookingStatus.IN_PROGRESS
                );

                if (relatedBooking.isPresent()) {
                    Booking booking = relatedBooking.get();
                    String userId = session.getDriver().getUserId();

                    log.info("Found related booking #{} for session {}. Processing automatic wallet payment.",
                            booking.getId(), session.getSessionId());

                    try {
                        // Auto-deduct charging cost from wallet
                        walletService.debit(
                                userId,
                                (double) cost,
                                TransactionType.CHARGING_PAYMENT,
                                String.format("Auto-payment for charging session %s", session.getSessionId()),
                                null,
                                session.getSessionId()
                        );

                        // Refund booking deposit
                        walletService.credit(
                                userId,
                                booking.getDepositAmount(),
                                TransactionType.BOOKING_DEPOSIT_REFUND,
                                String.format("Deposit refund for booking #%d", booking.getId()),
                                null,
                                null,
                                booking.getId(),
                                session.getSessionId()
                        );

                        // Update payment status to COMPLETED
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaymentMethod(Payment.PaymentMethod.WALLET);
                        payment.setPaidAt(LocalDateTime.now());
                        paymentRepository.save(payment);

                        // Update booking status to COMPLETED
                        booking.setBookingStatus(BookingStatus.COMPLETED);
                        bookingRepository.save(booking);

                        log.info("Successfully auto-processed payment for session {}. Charged: {} VND, Refunded deposit: {} VND",
                                session.getSessionId(), cost, booking.getDepositAmount());

                    } catch (Exception e) {
                        log.error("Failed to auto-process wallet payment for session {}: {}. Payment remains UNPAID.",
                                session.getSessionId(), e.getMessage(), e);
                        // Payment remains UNPAID, user needs to manually pay
                    }
                }
                // Note: Email will be sent by caller after transaction commits
                // to avoid transaction issues with async operations
            }
        }

        chargingSessionRepository.saveAndFlush(session);
        log.info("Session {} stopped. Status: {}. Cost: {}. Energy: {} kWh. Duration: {} min",
            session.getSessionId(), finalStatus, cost, session.getEnergyKwh(), session.getDurationMin());
    }

    /**
     * Send completion email after transaction commits
     * This method loads necessary entities and sends email asynchronously
     */
    private void sendCompletionEmailAsync(ChargingSession session) {
        try {
            // Eager load entities before async email call
            Driver driver = session.getDriver();
            if (driver != null && driver.getUser() != null) {
                driver.getUser().getEmail(); // Force load
            }
            ChargingPoint point = session.getChargingPoint();
            if (point != null && point.getStation() != null) {
                point.getStation().getName(); // Force load
                point.getStation().getAddress(); // Force load
            }

            emailService.sendChargingCompleteEmail(session);
        } catch (Exception e) {
            log.error("Failed to send completion email for session {}: {}", session.getSessionId(), e.getMessage());
        }
    }
}

