package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Plan;
import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.entity.Payment;
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

    // Phase 2: background simulation tick, runs every 2 seconds
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void simulateChargingTick() {
        List<ChargingSession> activeSessions = chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);

        // Only log when there are active sessions to avoid console spam
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

                // Simulate 6 giây một tick
                float energyPerTick = powerKw * (6.0f / 3600.0f); //6 giây/3600 giây trong 1 giờ (đổi giây sang giờ)
                float socAddedPerTick = (energyPerTick / capacityKwh) * 100.0f;

                float currentSoc = session.getEndSocPercent();
                float newSocFloat = currentSoc + socAddedPerTick;
                int newSocRounded = Math.round(newSocFloat);

                // Update session counters (add 6 simulated seconds = 0.1 minute)
                session.setDurationMin(session.getDurationMin() + 0.1f);
                session.setEnergyKwh(session.getEnergyKwh() + energyPerTick);

                if (newSocRounded >= targetSoc) {
                    // Cap at target and stop
                    session.setEndSocPercent(targetSoc);
                    vehicle.setCurrentSocPercent(targetSoc);
                    stopSessionLogic(session, ChargingSessionStatus.COMPLETED);
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

        session.setStatus(finalStatus);
        session.setEndTime(LocalDateTime.now());

        // Calculate cost - use default plan or fallback
        Plan plan = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
        float cost = 0f;
        if (plan != null) {
            cost = (session.getEnergyKwh() * plan.getPricePerKwh()) + (session.getDurationMin() * plan.getPricePerMinute());
        }
        session.setCostTotal(cost);

        // Release charging point
        ChargingPoint point = session.getChargingPoint();
        if (point != null) {
            point.setStatus(ChargingPointStatus.AVAILABLE);
            point.setCurrentSession(null);
            chargingPointRepository.save(point);
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
                log.info("Created UNPAID payment for completed session {}", session.getSessionId());

            // Gửi email thông báo kết thúc sạc
            emailService.sendChargingCompleteEmail(session);
            }
        }

        chargingSessionRepository.save(session);
        log.info("Session {} stopped. Status: {}. Cost: {}", session.getSessionId(), finalStatus, cost);
    }
}
