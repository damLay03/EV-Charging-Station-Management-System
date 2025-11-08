package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.enums.PaymentStatus;
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

        // Calculate cost - use default plan or fallback
        Plan plan = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
        float cost = 0f;
        if (plan != null) {
            cost = (session.getEnergyKwh() * plan.getPricePerKwh()) + (session.getDurationMin() * plan.getPricePerMinute());
            log.info("Calculated cost for session {}: {} kWh * {} + {} min * {} = {}",
                session.getSessionId(),
                session.getEnergyKwh(),
                plan.getPricePerKwh(),
                session.getDurationMin(),
                plan.getPricePerMinute(),
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

