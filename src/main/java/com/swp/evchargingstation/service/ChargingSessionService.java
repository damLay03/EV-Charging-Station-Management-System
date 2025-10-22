package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.ChargingSessionResponse;
import com.swp.evchargingstation.dto.response.driver.DriverDashboardResponse;
import com.swp.evchargingstation.dto.response.MonthlyAnalyticsResponse;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.VehicleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    VehicleRepository vehicleRepository;

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
}
