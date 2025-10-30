package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.HourlyUsageData;
import com.swp.evchargingstation.dto.response.StationUsageResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.Station;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.StationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationUsageService {

    StationRepository stationRepository;
    ChargingPointRepository chargingPointRepository;
    ChargingSessionRepository chargingSessionRepository;

    /**
     * Lấy mức độ sử dụng của một trạm trong ngày (mặc định: hôm nay)
     */
    @Transactional(readOnly = true)
    public StationUsageResponse getStationUsageToday(String stationId) {
        return getStationUsageByDate(stationId, LocalDate.now());
    }

    /**
     * Lấy mức độ sử dụng của một trạm theo ngày cụ thể
     */
    @Transactional(readOnly = true)
    public StationUsageResponse getStationUsageByDate(String stationId, LocalDate date) {
        log.info("Fetching station usage for stationId={}, date={}", stationId, date);

        // 1. Kiểm tra trạm có tồn tại không
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // 2. Lấy thông tin charging points hiện tại
        List<ChargingPoint> allPoints = chargingPointRepository.findByStation_StationId(stationId);
        int totalPoints = allPoints.size();
        int currentInUse = (int) allPoints.stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.CHARGING)
                .count();
        int currentAvailable = (int) allPoints.stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.AVAILABLE)
                .count();

        double currentUsagePercent = totalPoints > 0 ? (currentInUse * 100.0 / totalPoints) : 0.0;

        // 3. Tính toán thống kê trong ngày
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Integer totalSessionsToday = chargingSessionRepository.countSessionsByStationAndTimeRange(
                stationId, startOfDay, endOfDay
        );
        totalSessionsToday = totalSessionsToday != null ? totalSessionsToday : 0;

        Integer completedSessionsToday = chargingSessionRepository.countSessionsByStationStatusAndTimeRange(
                stationId, ChargingSessionStatus.COMPLETED, startOfDay, endOfDay
        );
        completedSessionsToday = completedSessionsToday != null ? completedSessionsToday : 0;

        Integer activeSessionsToday = chargingSessionRepository.countSessionsByStationStatusAndTimeRange(
                stationId, ChargingSessionStatus.IN_PROGRESS, startOfDay, endOfDay
        );
        activeSessionsToday = activeSessionsToday != null ? activeSessionsToday : 0;

        Double totalEnergyToday = chargingSessionRepository.sumEnergyByStationAndTimeRange(
                stationId, startOfDay, endOfDay
        );
        totalEnergyToday = totalEnergyToday != null ? totalEnergyToday : 0.0;

        Double totalRevenueToday = chargingSessionRepository.sumRevenueByStationAndTimeRange(
                stationId, startOfDay, endOfDay
        );
        totalRevenueToday = totalRevenueToday != null ? totalRevenueToday : 0.0;

        // 4. Phân tích theo giờ
        List<HourlyUsageData> hourlyUsage = calculateHourlyUsage(stationId, date, totalPoints);

        // 5. Tìm giờ cao điểm
        Integer peakHour = findPeakHour(hourlyUsage);
        Double peakUsagePercent = findPeakUsagePercent(hourlyUsage);

        // 6. Build response
        return StationUsageResponse.builder()
                .stationId(stationId)
                .stationName(station.getName())
                .address(station.getAddress())
                .date(date)
                .totalChargingPoints(totalPoints)
                .currentInUsePoints(currentInUse)
                .currentAvailablePoints(currentAvailable)
                .currentUsagePercent(Math.round(currentUsagePercent * 100.0) / 100.0)
                .totalSessionsToday(totalSessionsToday)
                .completedSessionsToday(completedSessionsToday)
                .activeSessionsToday(activeSessionsToday)
                .totalEnergyToday(Math.round(totalEnergyToday * 100.0) / 100.0)
                .totalRevenueToday(Math.round(totalRevenueToday * 100.0) / 100.0)
                .hourlyUsage(hourlyUsage)
                .peakHour(peakHour)
                .peakUsagePercent(peakUsagePercent)
                .build();
    }

    /**
     * Lấy mức độ sử dụng của TẤT CẢ trạm trong ngày
     */
    @Transactional(readOnly = true)
    public List<StationUsageResponse> getAllStationsUsageToday() {
        return getAllStationsUsageByDate(LocalDate.now());
    }

    /**
     * Lấy mức độ sử dụng của TẤT CẢ trạm theo ngày cụ thể
     */
    @Transactional(readOnly = true)
    public List<StationUsageResponse> getAllStationsUsageByDate(LocalDate date) {
        log.info("Fetching all stations usage for date={}", date);

        List<Station> allStations = stationRepository.findAll();
        List<StationUsageResponse> responses = new ArrayList<>();

        for (Station station : allStations) {
            try {
                StationUsageResponse usage = getStationUsageByDate(station.getStationId(), date);
                responses.add(usage);
            } catch (Exception e) {
                log.error("Error fetching usage for station {}: {}", station.getStationId(), e.getMessage());
            }
        }

        return responses;
    }

    // ========== HELPER METHODS ==========

    /**
     * Tính toán mức độ sử dụng theo từng giờ (0-23)
     */
    private List<HourlyUsageData> calculateHourlyUsage(String stationId, LocalDate date, int totalPoints) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Lấy raw data từ DB
        List<Object[]> rawData = chargingSessionRepository.findHourlyUsageByStation(
                stationId, startOfDay, endOfDay
        );

        // Convert sang Map để dễ xử lý
        Map<Integer, HourlyUsageData> hourlyMap = new HashMap<>();
        if (rawData != null) {
            for (Object[] row : rawData) {
                if (row == null || row.length < 4) continue;

                Integer hour = (Integer) row[0];
                Long sessionCount = row[1] != null ? (Long) row[1] : 0L;
                Double energy = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                Double revenue = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

                // Tạm tính usage percent (có thể cần logic phức tạp hơn)
                // Giả sử: nếu có session trong giờ đó => coi như có điểm đang sạc
                // Cần logic chính xác hơn dựa trên thời gian bắt đầu/kết thúc session
                double usagePercent = totalPoints > 0 ? (sessionCount * 100.0 / totalPoints) : 0.0;

                HourlyUsageData hourlyData = HourlyUsageData.builder()
                        .hour(hour)
                        .sessionCount(sessionCount.intValue())
                        .activePoints(sessionCount.intValue()) // Simplified - cần logic chính xác hơn
                        .usagePercent(Math.round(usagePercent * 100.0) / 100.0)
                        .energyConsumed(Math.round(energy * 100.0) / 100.0)
                        .revenue(Math.round(revenue * 100.0) / 100.0)
                        .build();

                hourlyMap.put(hour, hourlyData);
            }
        }

        // Tạo list đầy đủ 24 giờ (fill 0 cho giờ không có data)
        List<HourlyUsageData> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            if (hourlyMap.containsKey(hour)) {
                result.add(hourlyMap.get(hour));
            } else {
                result.add(HourlyUsageData.builder()
                        .hour(hour)
                        .sessionCount(0)
                        .activePoints(0)
                        .usagePercent(0.0)
                        .energyConsumed(0.0)
                        .revenue(0.0)
                        .build());
            }
        }

        return result;
    }

    /**
     * Tìm giờ cao điểm (giờ có nhiều session nhất)
     */
    private Integer findPeakHour(List<HourlyUsageData> hourlyUsage) {
        if (hourlyUsage == null || hourlyUsage.isEmpty()) {
            return null;
        }

        return hourlyUsage.stream()
                .max((h1, h2) -> Integer.compare(h1.getSessionCount(), h2.getSessionCount()))
                .map(HourlyUsageData::getHour)
                .orElse(null);
    }

    /**
     * Tìm % sử dụng cao nhất trong ngày
     */
    private Double findPeakUsagePercent(List<HourlyUsageData> hourlyUsage) {
        if (hourlyUsage == null || hourlyUsage.isEmpty()) {
            return 0.0;
        }

        return hourlyUsage.stream()
                .map(HourlyUsageData::getUsagePercent)
                .max(Double::compare)
                .orElse(0.0);
    }
}