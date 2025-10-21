package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StartChargingRequest;
import com.swp.evchargingstation.dto.response.ChargingProgressResponse;
import com.swp.evchargingstation.dto.response.ChargingSimulationResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChargingSimulationService {

    ChargingSessionRepository chargingSessionRepository;
    ChargingPointRepository chargingPointRepository;
    VehicleRepository vehicleRepository;
    DriverRepository driverRepository;
    UserRepository userRepository;

    // Lưu trữ trạng thái giả lập trong bộ nhớ (trong thực tế có thể dùng Redis)
    static final Map<String, SimulationState> activeSimulations = new HashMap<>();

    // Giá điện mặc định (VNĐ/kWh)
    static final float DEFAULT_PRICE_PER_KWH = 3500f;

    /**
     * Bắt đầu phiên sạc mới
     */
    @Transactional
    public ChargingProgressResponse startCharging(StartChargingRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} starting charging session for vehicle {}", userId, request.getVehicleId());

        // Validate driver
        Driver driver = driverRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Kiểm tra vehicle có thuộc về driver không
        if (!vehicle.getOwner().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate charging point
        ChargingPoint chargingPoint = chargingPointRepository.findById(request.getChargingPointId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));

        // Kiểm tra charging point có available không
        if (chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
            throw new AppException(ErrorCode.CHARGING_POINT_NOT_AVAILABLE);
        }

        // Validate SOC
        if (request.getStartSocPercent() >= request.getTargetSocPercent()) {
            throw new AppException(ErrorCode.INVALID_SOC_RANGE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tạo charging session
        ChargingSession session = ChargingSession.builder()
                .driver(driver)
                .vehicle(vehicle)
                .chargingPoint(chargingPoint)
                .startTime(LocalDateTime.now())
                .startSocPercent(request.getStartSocPercent())
                .endSocPercent(request.getStartSocPercent()) // Khởi tạo bằng start
                .energyKwh(0)
                .durationMin(0)
                .costTotal(0)
                .startedByUser(user)
                .status(ChargingSessionStatus.IN_PROGRESS)
                .build();

        session = chargingSessionRepository.save(session);

        // Cập nhật trạng thái charging point
        chargingPoint.setStatus(ChargingPointStatus.CHARGING);
        chargingPoint.setCurrentSession(session);
        chargingPointRepository.save(chargingPoint);

        // Khởi tạo trạng thái giả lập
        SimulationState simState = new SimulationState();
        simState.sessionId = session.getSessionId();
        simState.vehicleId = vehicle.getVehicleId();
        simState.chargingPointId = chargingPoint.getPointId();
        simState.startTime = LocalDateTime.now();
        simState.currentSocPercent = request.getStartSocPercent();
        simState.targetSocPercent = request.getTargetSocPercent();
        simState.batteryCapacityKwh = vehicle.getBatteryCapacityKwh();
        simState.maxPowerKw = chargingPoint.getChargingPower().getPowerKw();
        simState.pricePerKwh = DEFAULT_PRICE_PER_KWH;
        simState.energyDeliveredKwh = 0f;
        simState.currentCost = 0f;

        activeSimulations.put(session.getSessionId(), simState);

        log.info("Charging session {} started successfully", session.getSessionId());

        return buildProgressResponse(session, simState);
    }

    /**
     * Giả lập tiến trình sạc (tăng SOC theo thời gian)
     * Được gọi định kỳ hoặc theo yêu cầu
     */
    @Transactional
    public ChargingSimulationResponse simulateChargingProgress(String sessionId) {
        log.info("Simulating charging progress for session {}", sessionId);

        SimulationState simState = activeSimulations.get(sessionId);
        if (simState == null) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND);
        }

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_ACTIVE);
        }

        // Tính thời gian đã trôi qua (phút)
        long elapsedMinutes = ChronoUnit.MINUTES.between(simState.startTime, LocalDateTime.now());

        // Giả lập: mỗi phút sạc tăng khoảng 1-2% SOC (tùy thuộc vào công suất)
        // Công thức: energy = power * time (kWh = kW * hours)
        float elapsedHours = elapsedMinutes / 60f;
        float energyDelivered = simState.maxPowerKw * elapsedHours;

        // Tính % SOC tăng dựa trên năng lượng
        float socIncrease = (energyDelivered / simState.batteryCapacityKwh) * 100f;
        int newSocPercent = (int) Math.min(simState.targetSocPercent,
                                          simState.currentSocPercent + socIncrease);

        // Cập nhật trạng thái giả lập
        simState.currentSocPercent = newSocPercent;
        simState.energyDeliveredKwh = energyDelivered;
        simState.currentCost = energyDelivered * simState.pricePerKwh;

        // Cập nhật session
        session.setEndSocPercent(newSocPercent);
        session.setEnergyKwh(energyDelivered);
        session.setDurationMin((int) elapsedMinutes);
        session.setCostTotal(simState.currentCost);

        boolean isCompleted = newSocPercent >= simState.targetSocPercent;

        if (isCompleted) {
            session.setStatus(ChargingSessionStatus.COMPLETED);
            session.setEndTime(LocalDateTime.now());

            // Cập nhật charging point về AVAILABLE
            ChargingPoint chargingPoint = session.getChargingPoint();
            chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
            chargingPoint.setCurrentSession(null);
            chargingPointRepository.save(chargingPoint);

            // Xóa khỏi active simulations
            activeSimulations.remove(sessionId);

            log.info("Charging session {} completed", sessionId);
        }

        chargingSessionRepository.save(session);

        return ChargingSimulationResponse.builder()
                .sessionId(sessionId)
                .message(isCompleted ? "Charging completed" : "Charging in progress")
                .currentSocPercent(newSocPercent)
                .energyDeliveredKwh(energyDelivered)
                .currentCost(simState.currentCost)
                .isCompleted(isCompleted)
                .build();
    }

    /**
     * Lấy tiến trình sạc hiện tại
     */
    public ChargingProgressResponse getChargingProgress(String sessionId) {
        log.info("Getting charging progress for session {}", sessionId);

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra quyền truy cập
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        if (!session.getDriver().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        SimulationState simState = activeSimulations.get(sessionId);

        // Nếu session đã hoàn thành, tạo response từ session data
        if (simState == null || session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            return buildCompletedProgressResponse(session);
        }

        return buildProgressResponse(session, simState);
    }

    /**
     * Dừng phiên sạc trước khi đạt target
     */
    @Transactional
    public ChargingProgressResponse stopCharging(String sessionId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} stopping charging session {}", userId, sessionId);

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra quyền
        if (!session.getDriver().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_ACTIVE);
        }

        // Cập nhật lần cuối trước khi dừng
        simulateChargingProgress(sessionId);

        // Cập nhật trạng thái
        session.setStatus(ChargingSessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        chargingSessionRepository.save(session);

        // Giải phóng charging point
        ChargingPoint chargingPoint = session.getChargingPoint();
        chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
        chargingPoint.setCurrentSession(null);
        chargingPointRepository.save(chargingPoint);

        // Xóa simulation state
        activeSimulations.remove(sessionId);

        log.info("Charging session {} stopped successfully", sessionId);

        return buildCompletedProgressResponse(session);
    }

    /**
     * Xây dựng response cho session đang active
     */
    private ChargingProgressResponse buildProgressResponse(ChargingSession session, SimulationState simState) {
        long elapsedMinutes = ChronoUnit.MINUTES.between(simState.startTime, LocalDateTime.now());

        // Ước tính năng lượng cần thiết để đạt target
        float remainingSocPercent = simState.targetSocPercent - simState.currentSocPercent;
        float estimatedTotalEnergy = (simState.targetSocPercent - session.getStartSocPercent())
                                     * simState.batteryCapacityKwh / 100f;

        // Ước tính thời gian còn lại (phút)
        float remainingEnergy = remainingSocPercent * simState.batteryCapacityKwh / 100f;
        int estimatedRemainingMinutes = (int) ((remainingEnergy / simState.maxPowerKw) * 60);

        int progressPercent = (int) (((float) (simState.currentSocPercent - session.getStartSocPercent())
                                     / (simState.targetSocPercent - session.getStartSocPercent())) * 100);

        return ChargingProgressResponse.builder()
                .sessionId(session.getSessionId())
                .vehicleId(session.getVehicle().getVehicleId())
                .vehicleLicensePlate(session.getVehicle().getLicensePlate())
                .vehicleModel(session.getVehicle().getModel())
                .chargingPointId(session.getChargingPoint().getPointId())
                .stationName(session.getChargingPoint().getStation().getName())
                .maxPowerKw(simState.maxPowerKw)
                .startTime(session.getStartTime())
                .startSocPercent(session.getStartSocPercent())
                .currentSocPercent(simState.currentSocPercent)
                .targetSocPercent(simState.targetSocPercent)
                .energyDeliveredKwh(simState.energyDeliveredKwh)
                .estimatedTotalEnergyKwh(estimatedTotalEnergy)
                .progressPercent(Math.min(100, progressPercent))
                .elapsedTimeMinutes((int) elapsedMinutes)
                .estimatedRemainingMinutes(Math.max(0, estimatedRemainingMinutes))
                .currentCost(simState.currentCost)
                .estimatedTotalCost(estimatedTotalEnergy * simState.pricePerKwh)
                .pricePerKwh(simState.pricePerKwh)
                .status(session.getStatus())
                .statusMessage("Đang sạc")
                .build();
    }

    /**
     * Xây dựng response cho session đã hoàn thành
     */
    private ChargingProgressResponse buildCompletedProgressResponse(ChargingSession session) {
        return ChargingProgressResponse.builder()
                .sessionId(session.getSessionId())
                .vehicleId(session.getVehicle().getVehicleId())
                .vehicleLicensePlate(session.getVehicle().getLicensePlate())
                .vehicleModel(session.getVehicle().getModel())
                .chargingPointId(session.getChargingPoint().getPointId())
                .stationName(session.getChargingPoint().getStation().getName())
                .maxPowerKw(session.getChargingPoint().getChargingPower().getPowerKw())
                .startTime(session.getStartTime())
                .startSocPercent(session.getStartSocPercent())
                .currentSocPercent(session.getEndSocPercent())
                .targetSocPercent(session.getEndSocPercent())
                .energyDeliveredKwh(session.getEnergyKwh())
                .estimatedTotalEnergyKwh(session.getEnergyKwh())
                .progressPercent(100)
                .elapsedTimeMinutes(session.getDurationMin())
                .estimatedRemainingMinutes(0)
                .currentCost(session.getCostTotal())
                .estimatedTotalCost(session.getCostTotal())
                .pricePerKwh(DEFAULT_PRICE_PER_KWH)
                .status(session.getStatus())
                .statusMessage(session.getStatus() == ChargingSessionStatus.COMPLETED ?
                              "Hoàn thành" : "Đã dừng")
                .build();
    }

    /**
     * Inner class để lưu trạng thái giả lập
     */
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class SimulationState {
        String sessionId;
        String vehicleId;
        String chargingPointId;
        LocalDateTime startTime;
        int currentSocPercent;
        int targetSocPercent;
        float batteryCapacityKwh;
        float maxPowerKw;
        float pricePerKwh;
        float energyDeliveredKwh;
        float currentCost;
    }
}
