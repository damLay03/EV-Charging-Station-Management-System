package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ChargingSessionResponse;
import com.swp.evchargingstation.dto.response.DriverDashboardResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChargingSessionService {

    ChargingSessionRepository chargingSessionRepository;
    DriverRepository driverRepository;
    UserRepository userRepository;
    VehicleRepository vehicleRepository;
    ChargingPointRepository chargingPointRepository;
    PlanRepository planRepository;
    PaymentRepository paymentRepository;
    StaffRepository staffRepository;

    ChargingSimulatorService simulatorService;
    EmailService emailService;

    /**
     * Lấy dashboard overview của driver đang đăng nhập
     */
    public DriverDashboardResponse getMyDashboard() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lấy userId từ JWT claims thay vì getName() (getName() trả về email)
        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Getting dashboard for driver: {}", userId);

        Driver driver = driverRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lấy thống kê từ charging sessions
        Integer totalSessions = chargingSessionRepository.countByDriverId(userId);
        Double totalCost = chargingSessionRepository.sumTotalSpentByDriverId(userId);

        // Tính tổng năng lượng
        Double totalEnergy = chargingSessionRepository.sumTotalEnergyByDriverId(userId);

        // Tính TB/tháng
        long monthsSinceJoin = ChronoUnit.MONTHS.between(driver.getJoinDate(), LocalDateTime.now());
        if (monthsSinceJoin == 0) monthsSinceJoin = 1; // Tránh chia cho 0
        String avgCostPerMonth = String.format("%.0f", totalCost / monthsSinceJoin);

        // Lấy thông tin xe chính (xe đầu tiên của driver)
        List<Vehicle> vehicles = vehicleRepository.findByOwner_UserId(userId);
        String vehicleModel = "";
        String licensePlate = "";
        Integer currentBatterySoc = 0;

        if (!vehicles.isEmpty()) {
            Vehicle primaryVehicle = vehicles.get(0);
            vehicleModel = primaryVehicle.getModel() != null ? primaryVehicle.getModel().getModelName() : "";
            licensePlate = primaryVehicle.getLicensePlate();

            // Lấy % pin từ session gần nhất
            currentBatterySoc = getLatestBatterySoc(userId);
        }

        return DriverDashboardResponse.builder()
                .totalCost(totalCost)
                .totalEnergyKwh(totalEnergy)
                .totalSessions(totalSessions)
                .averageCostPerMonth(avgCostPerMonth)
                .vehicleModel(vehicleModel)
                .licensePlate(licensePlate)
                .currentBatterySoc(currentBatterySoc)
                .build();
    }

    /**
     * Lấy danh sách lịch sử phiên sạc của driver
     */
    public List<ChargingSessionResponse> getMySessions() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lấy userId từ JWT claims thay vì getName() (getName() trả về email)
        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Getting charging sessions for driver: {}", userId);

        List<ChargingSession> sessions = chargingSessionRepository.findByDriverIdOrderByStartTimeDesc(userId);

        return sessions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một phiên sạc
     */
    public ChargingSessionResponse getSessionById(String sessionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lấy userId từ JWT claims thay vì getName() (getName() trả về email)
        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        // Kiểm tra quyền truy cập
        if (!session.getDriver().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return convertToResponse(session);
    }

    /**
     * Chuyển đổi ChargingSession entity sang ChargingSessionResponse
     */
    private com.swp.evchargingstation.dto.response.ChargingSessionResponse convertToResponse(ChargingSession session) {
        String stationName = "";
        String stationAddress = "";
        String chargingPointName = "";
        String powerOutput = "N/A";

        if (session.getChargingPoint() != null) {
            try {
                ChargingPoint point = session.getChargingPoint();
                // ChargingPoint không có name, dùng pointId nếu name null
                chargingPointName = point.getName() != null ? point.getName() : (point.getPointId() != null ? point.getPointId() : "");

                if (point.getStation() != null) {
                    try {
                        stationName = point.getStation().getName();
                        stationAddress = point.getStation().getAddress();
                    } catch (Exception e) {
                        log.warn("Could not load station details: {}", e.getMessage());
                    }
                }

                if (point.getChargingPower() != null) {
                    try {
                        powerOutput = point.getChargingPower().name().replace("_", " ");
                    } catch (Exception ignore) {
                        powerOutput = "N/A";
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load charging point details: {}", e.getMessage());
            }
        }

        // Realtime fields calculation
        int targetSoc = session.getTargetSocPercent() != null ? session.getTargetSocPercent() : 100;
        int startSoc = session.getStartSocPercent();
        int currentSoc;
        float elapsedMinutes;
        Integer estimatedTimeRemaining = null;
        float energyConsumed;

        // Use hardcoded fallback to avoid query triggering auto-flush during entity modifications
        float pricePerKwh = 3800f; // Default "Linh hoạt" plan price
        float pricePerMinute = 0f; // Default "Linh hoạt" plan price per minute
        try {
            Plan plan = planRepository.findByNameIgnoreCase("Linh hoạt").orElse(null);
            if (plan != null) {
                pricePerKwh = plan.getPricePerKwh();
                pricePerMinute = plan.getPricePerMinute();
            }
        } catch (Exception e) {
            log.warn("Could not fetch plan price, using default: {}", e.getMessage());
        }
        float currentCost;

        Vehicle vehicle = session.getVehicle();
        float batteryCapacity = vehicle != null ? vehicle.getBatteryCapacityKwh() : 0f;
        float energyPerPercent = batteryCapacity > 0 ? (batteryCapacity / 100f) : 0f;

        if (session.getStatus() == com.swp.evchargingstation.enums.ChargingSessionStatus.IN_PROGRESS) {
            // For in-progress sessions, use real-time data
            currentSoc = (vehicle != null && vehicle.getCurrentSocPercent() != null) ? vehicle.getCurrentSocPercent() : session.getEndSocPercent();
            elapsedMinutes = (float) java.time.temporal.ChronoUnit.MINUTES.between(session.getStartTime(), java.time.LocalDateTime.now());

            // Calculate energy consumed based on SOC gain
            int socGained = Math.max(0, currentSoc - startSoc);
            energyConsumed = socGained * energyPerPercent;

            // Estimate time remaining
            if (currentSoc < targetSoc && socGained > 0 && elapsedMinutes > 0) {
                int remainingSoc = targetSoc - currentSoc;
                float avgSocPerMinute = socGained / elapsedMinutes;
                if (avgSocPerMinute > 0) {
                    estimatedTimeRemaining = (int) Math.ceil(remainingSoc / avgSocPerMinute);
                }
            }

            // Calculate current cost based on energy and time
            currentCost = (energyConsumed * pricePerKwh) + (elapsedMinutes * pricePerMinute);
        } else {
            // For completed sessions, use stored data
            currentSoc = session.getEndSocPercent();
            elapsedMinutes = session.getDurationMin();
            energyConsumed = session.getEnergyKwh();
            currentCost = session.getCostTotal();
        }

        // Lấy thông tin thanh toán
        com.swp.evchargingstation.entity.Payment payment = paymentRepository.findByChargingSession(session).orElse(null);
        Boolean isPaid = payment != null && payment.getStatus() == com.swp.evchargingstation.enums.PaymentStatus.COMPLETED;

        // paymentStatus: luôn có giá trị vì payment được tạo tự động khi session COMPLETED
        String paymentStatus = payment != null ? payment.getStatus().name() : "UNPAID";

        return com.swp.evchargingstation.dto.response.ChargingSessionResponse.builder()
                .sessionId(session.getSessionId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .durationMin(session.getDurationMin())
                .stationName(stationName)
                .stationAddress(stationAddress)
                .chargingPointName(chargingPointName)
                .startSocPercent(session.getStartSocPercent())
                .endSocPercent(session.getEndSocPercent())
                .energyKwh(session.getEnergyKwh())
                .costTotal(session.getCostTotal())
                .status(session.getStatus())
                .vehicleModel(session.getVehicle() != null && session.getVehicle().getModel() != null ? session.getVehicle().getModel().getModelName() : "")
                .licensePlate(session.getVehicle() != null ? session.getVehicle().getLicensePlate() : "")
                // realtime additions
                .currentSocPercent(currentSoc)
                .targetSocPercent(targetSoc)
                .elapsedTimeMinutes(elapsedMinutes)
                .estimatedTimeRemainingMinutes(estimatedTimeRemaining)
                .pricePerKwh(pricePerKwh)
                .energyConsumedKwh(energyConsumed)
                .currentCost(currentCost)
                .powerOutput(powerOutput)
                // payment status
                .isPaid(isPaid)
                .paymentStatus(paymentStatus)
                .build();
    }

    /**
     * Lấy % pin từ session gần nhất
     */
    private Integer getLatestBatterySoc(String driverId) {
        return chargingSessionRepository.findLatestEndSocByDriverId(driverId)
                .orElse(0);
    }
    /**
     * Lấy thống kê analytics theo tháng cho driver (5 tháng gần nhất)
     * Phục vụ cho tab "Phân tích" với 3 biểu đồ:
     * - Chi phí theo tháng (cột)
     * - Năng lượng tiêu thụ (đường)
     * - Số phiên sạc (cột)
     */
    public List<MonthlyAnalyticsResponse> getMyMonthlyAnalytics() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lấy userId từ JWT claims thay vì getName() (getName() trả về email)
        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Getting monthly analytics for driver: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        List<MonthlyAnalyticsResponse> analytics = new java.util.ArrayList<>();

        // Lấy data 5 tháng gần nhất (từ tháng hiện tại trở về trước)
        for (int i = 4; i >= 0; i--) {
            LocalDateTime targetDate = now.minusMonths(i);
            int year = targetDate.getYear();
            int month = targetDate.getMonthValue();

            Double totalCost = chargingSessionRepository.sumCostByDriverAndMonth(userId, year, month);
            Double totalEnergy = chargingSessionRepository.sumEnergyByDriverAndMonth(userId, year, month);
            Integer totalSessions = chargingSessionRepository.countSessionsByDriverAndMonth(userId, year, month);

            analytics.add(MonthlyAnalyticsResponse.builder()
                    .month(month)
                    .year(year)
                    .totalCost(totalCost)
                    .totalEnergyKwh(totalEnergy)
                    .totalSessions(totalSessions)
                    .monthLabel("T" + month)
                    .build());
        }

        return analytics;
    }

    // Phase 1: Start a new charging session
    @Transactional
    @PreAuthorize("hasRole('DRIVER')")
    public ChargingSessionResponse startSession(StartChargingRequest request, String driverId) {
        Integer target = request.getTargetSocPercent() != null ? request.getTargetSocPercent() : 100;

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        ChargingPoint chargingPoint = chargingPointRepository.findById(request.getChargingPointId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));

        // Validations
        if (chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
            throw new AppException(ErrorCode.CHARGING_POINT_NOT_AVAILABLE);
        }
        if (!vehicle.getOwner().getUserId().equals(driverId)) {
            throw new AppException(ErrorCode.VEHICLE_NOT_BELONG_TO_DRIVER);
        }
        if (vehicle.getCurrentSocPercent() >= target) {
            throw new AppException(ErrorCode.INVALID_SOC_RANGE);
        }

        // Create session
        int currentSoc = vehicle.getCurrentSocPercent() != null ? vehicle.getCurrentSocPercent() : 0;

        ChargingSession newSession = ChargingSession.builder()
                .driver(driver)
                .vehicle(vehicle)
                .chargingPoint(chargingPoint)
                .startTime(LocalDateTime.now())
                .startSocPercent(currentSoc)
                .endSocPercent(currentSoc)  // Initialize with current SOC
                .targetSocPercent(target)
                .energyKwh(0f)
                .durationMin(0f)
                .costTotal(0f)
                .startedByUser(driver.getUser())
                .status(ChargingSessionStatus.IN_PROGRESS)
                .build();

        chargingSessionRepository.saveAndFlush(newSession);

        log.info("Created charging session {} for driver {} at point {}. Start SOC: {}%, Target: {}%",
            newSession.getSessionId(), driverId, chargingPoint.getPointId(), currentSoc, target);

        // Update charging point -> CHARGING
        chargingPoint.setStatus(ChargingPointStatus.CHARGING);
        chargingPoint.setCurrentSession(newSession);
        chargingPointRepository.save(chargingPoint);

        // Eager load entities before async email call to avoid LazyInitializationException
        // Force Hibernate to load the lazy entities within the transaction
        User driverUser = driver.getUser();
        if (driverUser != null) {
            driverUser.getEmail(); // Force load
        }
        if (chargingPoint.getStation() != null) {
            chargingPoint.getStation().getName(); // Force load
        }

        // Gửi email thông báo bắt đầu sạc
        emailService.sendChargingStartEmail(newSession);


        log.info("Started charging session {} for driver {} at point {}", newSession.getSessionId(), driverId, chargingPoint.getPointId());
        return convertToResponse(newSession);
    }

    // Phase 3: Stop charging by user (cancel)
    @Transactional
    @PreAuthorize("hasRole('DRIVER')")
    public ChargingSessionResponse stopSessionByUser(String sessionId, String driverId) {
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        if (!session.getDriver().getUserId().equals(driverId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_ACTIVE);
        }

        // Eager load entities BEFORE stopSessionLogic to avoid lazy loading issues
        Driver driver = session.getDriver();
        if (driver != null && driver.getUser() != null) {
            driver.getUser().getEmail(); // Force load
        }
        ChargingPoint chargingPoint = session.getChargingPoint();
        if (chargingPoint != null && chargingPoint.getStation() != null) {
            chargingPoint.getStation().getName(); // Force load
            chargingPoint.getStation().getAddress(); // Force load
        }

        // Update session's endSocPercent from vehicle before stopping
        Vehicle vehicle = session.getVehicle();
        if (vehicle != null && vehicle.getCurrentSocPercent() != null) {
            session.setEndSocPercent(vehicle.getCurrentSocPercent());
            log.info("Updated session {} endSocPercent to {}% from vehicle before stopping",
                sessionId, vehicle.getCurrentSocPercent());
        }

        // Dừng thủ công cũng set status = COMPLETED (giống sạc đầy) để có thể thanh toán
        simulatorService.stopSessionLogic(session, ChargingSessionStatus.COMPLETED);

        // Send email after transaction (will be sent after method completes and transaction commits)
        // Reuse driver and chargingPoint variables already loaded above
        if (driver != null && driver.getUser() != null) {
            driver.getUser().getEmail(); // Force load for async email
        }
        if (chargingPoint != null && chargingPoint.getStation() != null) {
            chargingPoint.getStation().getName(); // Force load for async email
            chargingPoint.getStation().getAddress(); // Force load for async email
        }

        emailService.sendChargingCompleteEmail(session);

        log.info("Driver {} stopped session {} manually", driverId, sessionId);
        return convertToResponse(session);
    }

    // ==================== STAFF - MY STATION SESSIONS MANAGEMENT ====================

    /**
     * [STAFF] Lấy danh sách phiên sạc tại trạm của staff
     */
    @PreAuthorize("hasRole('STAFF')")
    public List<ChargingSessionResponse> getMyStationSessions() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Staff {} requesting charging sessions at their station", userId);

        com.swp.evchargingstation.entity.Staff staff = staffRepository.findByIdWithStation(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Lấy trạm mà staff quản lý
        com.swp.evchargingstation.entity.Station station = staff.getManagedStation();
        if (station == null) {
            throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
        }

        String stationId = station.getStationId();
        List<ChargingSession> sessions = chargingSessionRepository.findByStationIdOrderByStartTimeDesc(stationId);

        return sessions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * [STAFF] Lấy chi tiết một phiên sạc tại trạm của staff
     */
    @PreAuthorize("hasRole('STAFF')")
    public ChargingSessionResponse getMyStationSessionById(String sessionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Staff {} requesting session detail: {}", userId, sessionId);

        com.swp.evchargingstation.entity.Staff staff = staffRepository.findByIdWithStation(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        com.swp.evchargingstation.entity.Station station = staff.getManagedStation();
        if (station == null) {
            throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
        }

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra session có thuộc trạm của staff không
        if (session.getChargingPoint() == null ||
            !session.getChargingPoint().getStation().getStationId().equals(station.getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return convertToResponse(session);
    }

    /**
     * [STAFF] Dừng phiên sạc tại trạm của staff (khẩn cấp hoặc bảo trì)
     */
    @Transactional
    @PreAuthorize("hasRole('STAFF')")
    public ChargingSessionResponse stopMyStationSession(String sessionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Staff {} stopping session: {}", userId, sessionId);

        com.swp.evchargingstation.entity.Staff staff = staffRepository.findByIdWithStation(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        com.swp.evchargingstation.entity.Station station = staff.getManagedStation();
        if (station == null) {
            throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
        }

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra session có thuộc trạm của staff không
        if (session.getChargingPoint() == null ||
            !session.getChargingPoint().getStation().getStationId().equals(station.getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_ACTIVE);
        }

        // Update session's endSocPercent from vehicle before stopping
        Vehicle vehicle = session.getVehicle();
        if (vehicle != null && vehicle.getCurrentSocPercent() != null) {
            session.setEndSocPercent(vehicle.getCurrentSocPercent());
            log.info("Updated session {} endSocPercent to {}% from vehicle before stopping",
                sessionId, vehicle.getCurrentSocPercent());
        }

        // Dừng phiên sạc với trạng thái COMPLETED để có thể thanh toán
        simulatorService.stopSessionLogic(session, ChargingSessionStatus.COMPLETED);
        log.info("Staff {} stopped session {} at station {}", userId, sessionId, station.getStationId());

        return convertToResponse(session);
    }
}
