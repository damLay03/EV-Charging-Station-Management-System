package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.StationRevenueResponse;
import com.swp.evchargingstation.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RevenueService {

    PaymentRepository paymentRepository;

    public List<StationRevenueResponse> getWeeklyRevenue(Integer year, Integer week) {
        // Nếu không truyền tham số, lấy tuần hiện tại
        if (year == null || week == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            // Tính số tuần trong năm (mode 1: Thứ 2 là ngày đầu tuần)
            week = now.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        }

        log.info("Fetching revenue for year: {}, week: {}", year, week);

        List<Object[]> results = paymentRepository.findWeeklyRevenueByStation(year, week);
        List<StationRevenueResponse> responses = new ArrayList<>();

        for (Object[] result : results) {
            // Native query returns: station_id, name, address, week, year, sum, count
            // Need to handle potential type differences from native query
            StationRevenueResponse response = StationRevenueResponse.builder()
                    .stationId((String) result[0])
                    .stationName((String) result[1])
                    .address((String) result[2])
                    .week(((Number) result[3]).intValue())      // ← Cast to Number first
                    .year(((Number) result[4]).intValue())      // ← Cast to Number first
                    .totalRevenue(((Number) result[5]).floatValue())
                    .totalSessions(((Number) result[6]).intValue())
                    .build();
            responses.add(response);
        }

        log.info("Found {} stations with revenue for week {} of year {}", responses.size(), week, year);

        return responses;
    }

    public List<StationRevenueResponse> getMonthlyRevenue(Integer year, Integer month) {
        // Nếu không truyền tham số, lấy tháng hiện tại
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        log.info("Fetching revenue for year: {}, month: {}", year, month);

        List<Object[]> results = paymentRepository.findMonthlyRevenueByStation(year, month);
        List<StationRevenueResponse> responses = new ArrayList<>();

        for (Object[] result : results) {
            StationRevenueResponse response = StationRevenueResponse.builder()
                    .stationId((String) result[0])
                    .stationName((String) result[1])
                    .address((String) result[2])
                    .month((Integer) result[3])
                    .year((Integer) result[4])
                    .totalRevenue(((Number) result[5]).floatValue())
                    .totalSessions(((Number) result[6]).intValue())
                    .build();
            responses.add(response);
        }

        return responses;
    }

    public List<StationRevenueResponse> getYearlyRevenue(Integer year) {
        // Nếu không truyền tham số, lấy năm hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        log.info("Fetching revenue for year: {}", year);

        List<Object[]> results = paymentRepository.findYearlyRevenueByStation(year);
        List<StationRevenueResponse> responses = new ArrayList<>();

        for (Object[] result : results) {
            StationRevenueResponse response = StationRevenueResponse.builder()
                    .stationId((String) result[0])
                    .stationName((String) result[1])
                    .address((String) result[2])
                    .month((Integer) result[3])
                    .year((Integer) result[4])
                    .totalRevenue(((Number) result[5]).floatValue())
                    .totalSessions(((Number) result[6]).intValue())
                    .build();
            responses.add(response);
        }

        return responses;
    }
}