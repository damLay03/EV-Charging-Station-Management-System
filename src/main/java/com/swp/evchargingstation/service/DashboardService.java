package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DashboardService {

    ChargingSessionRepository chargingSessionRepository;

    /**
     * Lấy thông tin tóm tắt dashboard
     */
    public DashboardSummaryResponse getDashboardSummary(String period) {
        String driverId = getCurrentDriverId();
        LocalDateTime startTime = calculateStartTime(period);

        Object[] result = chargingSessionRepository.getSummaryByDriverAndStartTime(driverId, startTime);

        Double totalCost = (Double) result[0];
        Double totalEnergy = (Double) result[1];
        Long sessionCount = (Long) result[2];

        BigDecimal totalRevenue = BigDecimal.valueOf(totalCost != null ? totalCost : 0.0)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal averagePrice = BigDecimal.ZERO;
        if (totalEnergy != null && totalEnergy > 0) {
            averagePrice = BigDecimal.valueOf(totalCost / totalEnergy)
                    .setScale(0, RoundingMode.HALF_UP);
        }

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalEnergyUsed(totalEnergy != null ? totalEnergy : 0.0)
                .totalSessions(sessionCount != null ? sessionCount.intValue() : 0)
                .averagePricePerKwh(averagePrice)
                .build();
    }

    /**
     * Lấy thống kê theo giờ trong ngày
     */
    public List<HourlyChargingResponse> getHourlyChargingSessions(LocalDate date) {
        String driverId = getCurrentDriverId();

        if (date == null) {
            date = LocalDate.now();
        }

        List<Object[]> results = chargingSessionRepository.getHourlySessionsByDriverAndDate(driverId, date);

        // Tạo map để lưu kết quả theo giờ
        Map<Integer, HourlyChargingResponse> hourlyMap = new HashMap<>();

        // Khởi tạo tất cả 24 giờ với giá trị 0
        for (int hour = 0; hour < 24; hour++) {
            hourlyMap.put(hour, HourlyChargingResponse.builder()
                    .hour(String.format("%02d:00", hour))
                    .sessionCount(0)
                    .totalEnergy(0.0)
                    .build());
        }

        // Fill dữ liệu thực tế
        for (Object[] result : results) {
            Integer hour = (Integer) result[0];
            Long count = (Long) result[1];
            Double energy = (Double) result[2];

            hourlyMap.put(hour, HourlyChargingResponse.builder()
                    .hour(String.format("%02d:00", hour))
                    .sessionCount(count.intValue())
                    .totalEnergy(energy)
                    .build());
        }

        // Trả về danh sách theo thứ tự giờ
        return hourlyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách trạm yêu thích
     */
    public List<FavoriteStationResponse> getFavoriteStations(Integer limit) {
        String driverId = getCurrentDriverId();

        List<Object[]> results = chargingSessionRepository.getFavoriteStationsByDriver(driverId);

        return results.stream()
                .limit(limit)
                .map(result -> FavoriteStationResponse.builder()
                        .stationId((String) result[0])
                        .stationName((String) result[1])
                        .address((String) result[2])
                        .sessionCount(((Long) result[3]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê thói quen sạc
     */
    public ChargingStatisticsResponse getChargingStatistics() {
        String driverId = getCurrentDriverId();

        // Tính thời gian sạc trung bình
        Double avgTime = chargingSessionRepository.getAverageChargingTimeByDriver(driverId);
        Integer averageMinutes = avgTime != null ? avgTime.intValue() : 0;

        // Tìm giờ cao điểm
        List<Object[]> peakHoursData = chargingSessionRepository.getPeakHoursByDriver(driverId);
        String peakHours = calculatePeakHours(peakHoursData);

        // Tìm ngày trong tuần thường sạc
        List<Object[]> frequentDaysData = chargingSessionRepository.getMostFrequentDaysByDriver(driverId);
        String mostFrequentDays = calculateMostFrequentDays(frequentDaysData);

        return ChargingStatisticsResponse.builder()
                .averageChargingTimeMinutes(averageMinutes)
                .peakHours(peakHours)
                .mostFrequentDays(mostFrequentDays)
                .build();
    }

    // ========== HELPER METHODS ==========

    private String getCurrentDriverId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private LocalDateTime calculateStartTime(String period) {
        LocalDateTime now = LocalDateTime.now();

        switch (period.toLowerCase()) {
            case "today":
                return now.with(LocalTime.MIN);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            default:
                return now.minusMonths(1);
        }
    }

    private String calculatePeakHours(List<Object[]> peakHoursData) {
        if (peakHoursData.isEmpty()) {
            return "N/A";
        }

        // Lấy 2 giờ cao điểm nhất
        List<Integer> topHours = peakHoursData.stream()
                .limit(2)
                .map(result -> (Integer) result[0])
                .sorted()
                .collect(Collectors.toList());

        if (topHours.size() == 1) {
            return String.format("%02d:00 - %02d:00", topHours.get(0), topHours.get(0) + 1);
        } else if (topHours.size() >= 2) {
            return String.format("%02d:00 - %02d:00", topHours.get(0), topHours.get(topHours.size() - 1) + 1);
        }

        return "N/A";
    }

    private String calculateMostFrequentDays(List<Object[]> frequentDaysData) {
        if (frequentDaysData.isEmpty()) {
            return "N/A";
        }

        // Map MySQL DAYOFWEEK (1=CN, 2=T2, ..., 7=T7) sang tên ngày
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

        // Lấy 2 ngày phổ biến nhất
        List<String> topDays = frequentDaysData.stream()
                .limit(2)
                .map(result -> {
                    Integer dayOfWeek = (Integer) result[0];
                    // MySQL DAYOFWEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
                    return dayNames[dayOfWeek - 1];
                })
                .collect(Collectors.toList());

        return String.join(", ", topDays);
    }
}
