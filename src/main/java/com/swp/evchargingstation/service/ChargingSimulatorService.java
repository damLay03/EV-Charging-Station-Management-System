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

    // Phase 2: background simulation tick, runs every 1 second
    // Chạy mỗi 1 giây thực tế, giả lập 4 giây (tốc độ 4x)
    // Ví dụ: Sạc 180 phút thực tế chỉ mất 45 phút hệ thống
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void simulateChargingTick() {
        List<ChargingSession> activeSessions = chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);

        if (!activeSessions.isEmpty()) {
            log.debug("Running charging simulation for {} active sessions", activeSessions.size());
        }

        for (ChargingSession session : activeSessions) {
            try {
                // Get vehicle from repository to ensure it's managed by EntityManager
                Vehicle vehicle = session.getVehicle();
                if (vehicle == null) {
                    log.warn("Session {} has no vehicle. Skipping.", session.getSessionId());
                    continue;
                }

                // Refresh vehicle from database to ensure it's in managed state
                vehicle = vehicleRepository.findById(vehicle.getVehicleId())
                    .orElse(vehicle);

                ChargingPoint point = session.getChargingPoint();
                if (point == null) {
                    log.warn("Session {} has no charging point. Skipping.", session.getSessionId());
                    continue;
                }

                // Lấy công suất trụ sạc
                float chargingPointPowerKw = point.getChargingPower().getPowerKw();

                // Lấy công suất tối đa xe có thể nhận
                float vehicleMaxPowerKw = vehicle.getMaxChargingPowerKw();

                // Công suất thực tế = MIN(công suất trụ, công suất tối đa xe)
                float actualPowerKw = Math.min(chargingPointPowerKw, vehicleMaxPowerKw);

                log.debug("Session {}: Charging point power = {} kW, Vehicle max power = {} kW, Actual power = {} kW",
                    session.getSessionId(), chargingPointPowerKw, vehicleMaxPowerKw, actualPowerKw);

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

                // Giả lập: 1 giây thực = 4 giây giả lập (tốc độ 4x)
                // Mỗi tick (1 giây thực) = 4 giây giả lập = 4/60 phút = 0.0667 phút
                float timePerTickMinutes = 4.0f / 60.0f; // 4 giây = 0.0667 phút
                float timePerTickHours = timePerTickMinutes / 60.0f; // Chuyển phút sang giờ
                float energyPerTick = actualPowerKw * timePerTickHours; // kWh = kW × hours
                float socAddedPerTick = (energyPerTick / capacityKwh) * 100.0f;

                int currentSoc = session.getEndSocPercent();
                float newSocFloat = currentSoc + socAddedPerTick;
                int newSocRounded = Math.round(newSocFloat);

                log.debug("Session {}: Power={} kW, Capacity={} kWh, Time={} min, Energy={} kWh, SOC+={} %, New SOC={} %",
                    session.getSessionId(), actualPowerKw, capacityKwh, timePerTickMinutes,
                    energyPerTick, socAddedPerTick, newSocRounded);

                // Cập nhật thời gian giả lập: mỗi tick = 4 giây = 0.0667 phút
                session.setDurationMin(session.getDurationMin() + timePerTickMinutes);
                session.setEnergyKwh(session.getEnergyKwh() + energyPerTick);

                // Tính chi phí real-time dựa trên plan của driver
                Driver driver = session.getDriver();
                Plan driverPlan = driver != null ? driver.getPlan() : null;

                if (driverPlan == null) {
                    // Fallback to "Linh hoạt" plan
                    driverPlan = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
                }

                if (driverPlan != null) {
                    float currentCost = (session.getEnergyKwh() * driverPlan.getPricePerKwh())
                                      + (session.getDurationMin() * driverPlan.getPricePerMinute());
                    session.setCostTotal(currentCost);
                }

                if (newSocRounded >= targetSoc) {
                    // Đạt mục tiêu, dừng sạc
                    session.setEndSocPercent(targetSoc);
                    vehicle.setCurrentSocPercent(targetSoc);

                    // Save vehicle before stopping session to ensure SOC is updated
                    vehicleRepository.saveAndFlush(vehicle);

                    stopSessionLogic(session, ChargingSessionStatus.COMPLETED);

                    // Send email after session stopped (outside transaction)
                    sendCompletionEmailAsync(session);
                } else {
                    // Cập nhật SOC cho session và vehicle
                    log.debug("Before update - Vehicle {}: currentSoc was {}%, updating to {}%",
                        vehicle.getVehicleId(), vehicle.getCurrentSocPercent(), newSocRounded);

                    session.setEndSocPercent(newSocRounded);
                    vehicle.setCurrentSocPercent(newSocRounded);

                    // Flush both to database immediately for real-time updates
                    chargingSessionRepository.saveAndFlush(session);
                    vehicleRepository.saveAndFlush(vehicle);

                    log.info("✅ Session {} updated: SOC {}%, Energy {} kWh, Duration {} min, Cost {} VND",
                        session.getSessionId(), newSocRounded, session.getEnergyKwh(),
                        session.getDurationMin(), session.getCostTotal());

                    log.debug("After save - Vehicle {}: currentSoc is now {}%",
                        vehicle.getVehicleId(), vehicle.getCurrentSocPercent());
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
            // Refresh vehicle from database to ensure it's managed
            Vehicle managedVehicle = vehicleRepository.findById(vehicle.getVehicleId())
                .orElse(vehicle);

            log.info("Updating vehicle {} SOC from {}% to {}%",
                managedVehicle.getVehicleId(),
                managedVehicle.getCurrentSocPercent(),
                session.getEndSocPercent());

            managedVehicle.setCurrentSocPercent(session.getEndSocPercent());
            vehicleRepository.saveAndFlush(managedVehicle);

            log.info("✅ Vehicle {} SOC updated to {}%",
                managedVehicle.getVehicleId(),
                managedVehicle.getCurrentSocPercent());
        } else if (vehicle != null) {
            log.warn("Vehicle {} has invalid endSocPercent: {}", vehicle.getVehicleId(), session.getEndSocPercent());
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

