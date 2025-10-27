package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ChargingSessionResponse;
import com.swp.evchargingstation.dto.response.driver.DriverDashboardResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.repository.VehicleRepository;
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

    ChargingSimulatorService simulatorService;

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
    private ChargingSessionResponse convertToResponse(ChargingSession session) {
        String stationName = "";
        String stationAddress = "";
        String chargingPointName = "";

        if (session.getChargingPoint() != null) {
            // ChargingPoint không có name, dùng pointId
            chargingPointName = session.getChargingPoint().getPointId() != null ? session.getChargingPoint().getPointId() : "";

            if (session.getChargingPoint().getStation() != null) {
                stationName = session.getChargingPoint().getStation().getName();
                stationAddress = session.getChargingPoint().getStation().getAddress();
            }
        }

        return ChargingSessionResponse.builder()
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
                // map VehicleModel enum -> modelName string
                .vehicleModel(session.getVehicle() != null && session.getVehicle().getModel() != null ? session.getVehicle().getModel().getModelName() : "")
                .licensePlate(session.getVehicle() != null ? session.getVehicle().getLicensePlate() : "")
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
        int currentSoc = vehicle.getCurrentSocPercent();
        ChargingSession newSession = ChargingSession.builder()
                .driver(driver)
                .vehicle(vehicle)
                .chargingPoint(chargingPoint)
                .startTime(LocalDateTime.now())
                .startSocPercent(currentSoc)
                .endSocPercent(currentSoc)
                .targetSocPercent(target)
                .energyKwh(0f)
                .durationMin(0)
                .costTotal(0f)
                .startedByUser(driver.getUser())
                .status(ChargingSessionStatus.IN_PROGRESS)
                .build();

        chargingSessionRepository.save(newSession);

        // Update charging point -> CHARGING
        chargingPoint.setStatus(ChargingPointStatus.CHARGING);
        chargingPoint.setCurrentSession(newSession);
        chargingPointRepository.save(chargingPoint);

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

        simulatorService.stopSessionLogic(session, ChargingSessionStatus.CANCELLED);
        log.info("Driver {} stopped session {} manually", driverId, sessionId);
        return convertToResponse(session);
    }

    /**
     * Lấy thông tin real-time của phiên sạc đang diễn ra
     * API này được gọi liên tục từ frontend (polling mỗi 3-5 giây) để cập nhật UI
     *
     * @param sessionId - ID của session cần lấy thông tin (từ frontend)
     */
    public com.swp.evchargingstation.dto.response.ActiveChargingSessionResponse getActiveSession(String sessionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("Getting active charging session {} for driver: {}", sessionId, userId);

        // Tìm session theo ID
        ChargingSession activeSession = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra quyền truy cập - session phải thuộc về driver này
        if (!activeSession.getDriver().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra session phải đang IN_PROGRESS
        if (activeSession.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_ACTIVE);
        }

        // Tính toán các giá trị real-time
        LocalDateTime now = LocalDateTime.now();
        long elapsedMinutes = ChronoUnit.MINUTES.between(activeSession.getStartTime(), now);

        // Lấy current SOC từ vehicle (được cập nhật bởi simulator)
        int currentSoc = activeSession.getVehicle().getCurrentSocPercent();

        // Tính năng lượng đã tiêu thụ (giả sử mỗi 1% pin = energyPerPercent kWh)
        float batteryCapacity = activeSession.getVehicle().getModel().getBatteryCapacityKwh();
        float energyPerPercent = batteryCapacity / 100f;
        int socGained = currentSoc - activeSession.getStartSocPercent();
        float energyConsumed = socGained * energyPerPercent;

        // Lấy giá điện (giả sử default là 3000 VND/kWh, sau này sẽ lấy từ plan)
        float pricePerKwh = 3000f; // TODO: Lấy từ plan của driver

        // Tính chi phí hiện tại
        float currentCost = energyConsumed * pricePerKwh;

        // Ước tính thời gian còn lại
        Integer estimatedTimeRemaining = null;
        if (currentSoc < activeSession.getTargetSocPercent() && socGained > 0 && elapsedMinutes > 0) {
            int remainingSoc = activeSession.getTargetSocPercent() - currentSoc;
            float avgSocPerMinute = (float) socGained / elapsedMinutes;
            if (avgSocPerMinute > 0) {
                estimatedTimeRemaining = (int) (remainingSoc / avgSocPerMinute);
            }
        }

        // Lấy thông tin trạm và charging point
        String stationName = "";
        String stationLocation = "";
        String chargingPointName = "";
        String powerOutput = "N/A";

        if (activeSession.getChargingPoint() != null) {
            ChargingPoint point = activeSession.getChargingPoint();
            chargingPointName = point.getName() != null ? point.getName() : point.getPointId();

            if (point.getChargingPower() != null) {
                powerOutput = point.getChargingPower().name().replace("_", " ");
            }

            if (point.getStation() != null) {
                stationName = point.getStation().getName();
                stationLocation = point.getStation().getAddress();
            }
        }

        return com.swp.evchargingstation.dto.response.ActiveChargingSessionResponse.builder()
                .sessionId(activeSession.getSessionId())
                .stationName(stationName)
                .chargingPointName(chargingPointName)
                .stationLocation(stationLocation)
                .status(activeSession.getStatus().name())
                .currentSocPercent(currentSoc)
                .targetSocPercent(activeSession.getTargetSocPercent())
                .startSocPercent(activeSession.getStartSocPercent())
                .startTime(activeSession.getStartTime().toString())
                .elapsedTimeMinutes((int) elapsedMinutes)
                .estimatedTimeRemainingMinutes(estimatedTimeRemaining)
                .pricePerKwh(pricePerKwh)
                .energyConsumedKwh(energyConsumed)
                .currentCost(currentCost)
                .powerOutput(powerOutput)
                .build();
    }
}
