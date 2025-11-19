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
    PaymentSettlementService paymentSettlementService;

    private void ensureWalletExists(String userId) {
        try {
            walletService.getWallet(userId);
        } catch (Exception ex) {
            try {
                walletService.createWalletByUserId(userId);
                log.info("Created wallet for user {} (auto)", userId);
            } catch (Exception ignored) {
                // If already exists or cannot create, ignore; debit will throw later and be handled
            }
        }
    }

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
                // Reload latest state to avoid overwriting manual/staff stop updates
                session = chargingSessionRepository.findById(session.getSessionId()).orElse(null);
                if (session == null) {
                    continue;
                }
                if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
                    log.debug("Skip session {}: status is {} (no longer IN_PROGRESS)", session.getSessionId(), session.getStatus());
                    continue;
                }

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

                // Cập nhật thời gian & năng lượng tích lũy
                session.setDurationMin(session.getDurationMin() + timePerTickMinutes);
                session.setEnergyKwh(session.getEnergyKwh() + energyPerTick);

                // Tính SOC dựa trên tổng năng lượng đã nạp kể từ đầu phiên (ổn định hơn, tránh lỗi làm tròn)
                float energySinceStart = session.getEnergyKwh();
                float socIncreaseFromEnergy = (energySinceStart / capacityKwh) * 100.0f;
                float computedSoc = session.getStartSocPercent() + socIncreaseFromEnergy;
                int newSocRounded = Math.min(100, Math.round(computedSoc));

                log.debug("Session {} tick: +{} kWh (total {} kWh), +{} min (total {}), computed SOC={} (rounded {}%)",
                        session.getSessionId(), energyPerTick, session.getEnergyKwh(),
                        timePerTickMinutes, session.getDurationMin(), computedSoc, newSocRounded);

                // Tính chi phí real-time dựa trên plan của driver
                Driver driver = session.getDriver();
                Plan driverPlan = driver != null ? driver.getPlan() : null;
                if (driverPlan == null) {
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

                    // Refresh vehicle từ database để đảm bảo nó được quản lý bởi EntityManager
                    Vehicle managedVehicle = vehicleRepository.findById(vehicle.getVehicleId())
                        .orElseThrow(() -> new RuntimeException("Vehicle not found"));

                    managedVehicle.setCurrentSocPercent(targetSoc);

                    // Save vehicle and session before stopping to ensure SOC is updated
                    chargingSessionRepository.saveAndFlush(session);
                    vehicleRepository.saveAndFlush(managedVehicle);

                    log.info("🎯 Session {} reached target SOC {}%. Stopping session.", session.getSessionId(), targetSoc);

                    stopSessionLogic(session, ChargingSessionStatus.COMPLETED);

                    // Send email after session stopped (outside transaction)
                    sendCompletionEmailAsync(session);
                } else {
                    // Cập nhật SOC cho session và vehicle
                    session.setEndSocPercent(newSocRounded);

                    Vehicle managedVehicle = vehicleRepository.findById(vehicle.getVehicleId())
                            .orElseThrow(() -> new RuntimeException("Vehicle not found: "));
                    managedVehicle.setCurrentSocPercent(newSocRounded);

                    // Flush both to database immediately for real-time updates
                    chargingSessionRepository.saveAndFlush(session);
                    vehicleRepository.saveAndFlush(managedVehicle);

                    log.info("✅ Session {} updated: SOC {}%, Energy {} kWh, Duration {} min, Cost {} VND",
                            session.getSessionId(), newSocRounded, session.getEnergyKwh(),
                            session.getDurationMin(), session.getCostTotal());
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
            try {
                // Run settlement in a separate transaction; if it fails, don't rollback stop flow
                paymentSettlementService.settlePaymentForCompletedSession(session, cost);
            } catch (Exception ex) {
                log.error("Settlement failed for session {}: {}. Leaving payment UNPAID.", session.getSessionId(), ex.getMessage(), ex);
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
