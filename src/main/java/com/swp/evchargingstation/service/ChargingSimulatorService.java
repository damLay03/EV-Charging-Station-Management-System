package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ĐƠN GIẢN HÓA: Charging Simulator với cơ chế flag-based stop
 *
 * Nguyên tắc:
 * 1. Scheduler chỉ UPDATE session nếu status = IN_PROGRESS
 * 2. Stop thủ công = Đổi status thành COMPLETED ngay lập tức
 * 3. Scheduler thấy status != IN_PROGRESS → bỏ qua
 * 4. Không có transaction lồng nhau, không có REQUIRES_NEW
 * 5. Mỗi operation độc lập, transaction ngắn
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChargingSimulatorService {

    ChargingSessionRepository chargingSessionRepository;
    VehicleRepository vehicleRepository;
    ChargingPointRepository chargingPointRepository;
    PlanRepository planRepository;
    BookingRepository bookingRepository; // Added for BUG #2 fix
    EmailService emailService;
    PaymentSettlementService paymentSettlementService;

    // Track sessions being processed to avoid concurrent updates
    private static final Set<String> PROCESSING_SESSIONS = ConcurrentHashMap.newKeySet();

    /**
     * SCHEDULER: Chạy mỗi giây, update tất cả session IN_PROGRESS
     * Đơn giản: Chỉ UPDATE, không STOP. Stop do user hoặc auto complete trigger.
     */
    @Scheduled(fixedRate = 1000)
    public void simulateChargingTick() {
        List<ChargingSession> activeSessions = chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);

        for (ChargingSession session : activeSessions) {
            // Skip if already being processed
            if (PROCESSING_SESSIONS.contains(session.getSessionId())) {
                continue;
            }

            try {
                PROCESSING_SESSIONS.add(session.getSessionId());
                updateSessionProgress(session.getSessionId());
            } catch (Exception e) {
                log.error("Error updating session {}: {}", session.getSessionId(), e.getMessage());
            } finally {
                PROCESSING_SESSIONS.remove(session.getSessionId());
            }
        }
    }

    /**
     * UPDATE SESSION: Transaction độc lập, chỉ update progress
     */
    @Transactional
    public void updateSessionProgress(String sessionId) {
        // Fetch session với tất cả relationships để tránh lazy loading exception
        ChargingSession session = chargingSessionRepository.findByIdWithRelationships(sessionId).orElse(null);
        if (session == null || session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            return; // Session đã bị stop từ nơi khác
        }

        Vehicle vehicle = session.getVehicle();
        if (vehicle == null) {
            return;
        }

        ChargingPoint point = session.getChargingPoint();
        if (point == null || point.getChargingPower() == null) {
            return;
        }

        // Tính toán tiến trình sạc
        float actualPowerKw = Math.min(
            point.getChargingPower().getPowerKw(),
            vehicle.getMaxChargingPowerKw()
        );

        float capacityKwh = vehicle.getBatteryCapacityKwh();
        int targetSoc = session.getTargetSocPercent() != null ? session.getTargetSocPercent() : 100;

        if (capacityKwh <= 0) {
            return;
        }

        // Init endSoc nếu chưa có
        if (session.getEndSocPercent() == 0 && session.getStartSocPercent() > 0) {
            session.setEndSocPercent(session.getStartSocPercent());
        }

        // Mỗi tick = 4 giây giả lập
        float timePerTickMinutes = 4.0f / 60.0f;
        float timePerTickHours = timePerTickMinutes / 60.0f;
        float energyPerTick = actualPowerKw * timePerTickHours;

        // Cập nhật session
        session.setDurationMin(session.getDurationMin() + timePerTickMinutes);
        session.setEnergyKwh(session.getEnergyKwh() + energyPerTick);

        // Tính SOC mới
        float energySinceStart = session.getEnergyKwh();
        float socIncrease = (energySinceStart / capacityKwh) * 100.0f;
        int newSoc = Math.min(100, Math.round(session.getStartSocPercent() + socIncrease));

        session.setEndSocPercent(newSoc);
        vehicle.setCurrentSocPercent(newSoc);

        // Tính cost
        Plan plan = getPlanForSession(session);
        if (plan != null) {
            float cost = (session.getEnergyKwh() * plan.getPricePerKwh())
                       + (session.getDurationMin() * plan.getPricePerMinute());
            session.setCostTotal(cost);
        }

        // Lưu vào DB
        chargingSessionRepository.save(session);
        vehicleRepository.save(vehicle);

        log.debug("Updated session {}: SOC {}%, Energy {} kWh, Cost {} VND",
            sessionId, newSoc, session.getEnergyKwh(), session.getCostTotal());

        // Kiểm tra đạt target → auto complete
        if (newSoc >= targetSoc) {
            log.info("Session {} reached target {}%. Auto completing...", sessionId, targetSoc);
            // Gọi complete trong transaction mới để tránh conflict
            completeSessionAsync(sessionId);
        }
    }

    /**
     * COMPLETE SESSION: Chạy async để không block scheduler
     */
    public void completeSessionAsync(String sessionId) {
        try {
            Thread.sleep(100); // Đợi scheduler transaction commit
            completeSession(sessionId);
        } catch (Exception e) {
            log.error("Error completing session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * COMPLETE SESSION: Transaction độc lập
     * Dùng cho: Auto complete (đạt target) HOẶC manual stop
     */
    @Transactional
    public void completeSession(String sessionId) {
        ChargingSession session = chargingSessionRepository.findByIdWithRelationships(sessionId).orElse(null);
        if (session == null) {
            return;
        }

        // Nếu đã COMPLETED rồi thì thôi
        if (session.getStatus() == ChargingSessionStatus.COMPLETED) {
            log.info("Session {} already completed", sessionId);
            return;
        }

        log.info("Completing session {}", sessionId);

        // Set status = COMPLETED
        session.setStatus(ChargingSessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());

        // ✅ NEW: Update booking status to COMPLETED if session has booking
        if (session.getBooking() != null) {
            Booking booking = session.getBooking();
            booking.setBookingStatus(com.swp.evchargingstation.enums.BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            log.info("✅ Booking #{} marked as COMPLETED (linked to session {})",
                     booking.getId(), sessionId);
        }

        // Update vehicle SOC cuối cùng
        Vehicle vehicle = session.getVehicle();
        if (vehicle != null) {
            vehicle.setCurrentSocPercent(session.getEndSocPercent());
            vehicleRepository.save(vehicle);
        }

        // Release charging point
        ChargingPoint point = session.getChargingPoint();
        if (point != null) {
            // FIX BUG #2: Kiểm tra upcoming booking trước khi set AVAILABLE
            LocalDateTime now = LocalDateTime.now();
            List<Booking> upcomingBookings = bookingRepository.findUpcomingBookingsForPoint(
                point.getPointId(),
                now,
                now.plusMinutes(30) // Check booking trong 30 phút tới
            );

            if (!upcomingBookings.isEmpty()) {
                // Có booking sắp tới → Giữ RESERVED thay vì AVAILABLE
                point.setStatus(ChargingPointStatus.RESERVED);
                log.info("Keeping point {} RESERVED due to upcoming booking at {}",
                         point.getName(), upcomingBookings.getFirst().getBookingTime());
            } else {
                // Không có booking gần → Set AVAILABLE
                point.setStatus(ChargingPointStatus.AVAILABLE);
            }

            point.setCurrentSession(null);
            chargingPointRepository.save(point);
        }

        // Tính cost cuối cùng
        Plan plan = getPlanForSession(session);
        if (plan != null) {
            float finalCost = (session.getEnergyKwh() * plan.getPricePerKwh())
                            + (session.getDurationMin() * plan.getPricePerMinute());
            session.setCostTotal(finalCost);
        }

        // Lưu session
        chargingSessionRepository.save(session);

        log.info("Session {} completed: SOC {}%, Energy {} kWh, Cost {} VND",
            sessionId, session.getEndSocPercent(), session.getEnergyKwh(), session.getCostTotal());

        // Settlement & Email (fire and forget)
        try {
            paymentSettlementService.settlePaymentForCompletedSession(session, session.getCostTotal());
        } catch (Exception e) {
            log.warn("Settlement failed for {}: {}", sessionId, e.getMessage());
        }

        try {
            emailService.sendChargingCompleteEmail(session);
        } catch (Exception e) {
            log.warn("Email failed for {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Helper: Lấy plan cho session
     */
    private Plan getPlanForSession(ChargingSession session) {
        Driver driver = session.getDriver();
        Plan plan = driver != null ? driver.getPlan() : null;

        if (plan == null) {
            plan = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
        }

        return plan;
    }
}
