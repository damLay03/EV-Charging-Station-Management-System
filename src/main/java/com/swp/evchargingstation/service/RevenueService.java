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

    public List<StationRevenueResponse> getWeeklyRevenue(Integer year, Integer month, Integer week) {
        // Nếu không truyền tham số, lấy tuần hiện tại
        if (year == null || month == null || week == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
            week = (now.getDayOfMonth() - 1) / 7 + 1; // Tính tuần trong tháng
        }

        log.info("Fetching revenue for year: {}, month: {}, week: {}", year, month, week);

        List<Object[]> results = paymentRepository.findWeeklyRevenueByStation(year, month, week);
        List<StationRevenueResponse> responses = new ArrayList<>();

        for (Object[] result : results) {
            StationRevenueResponse response = StationRevenueResponse.builder()
                    .stationId((String) result[0])
                    .stationName((String) result[1])
                    .address((String) result[2])
                    .month((Integer) result[3])
                    .year((Integer) result[4])
                    .week((Integer) result[5])
                    .totalRevenue(((Number) result[6]).floatValue())
                    .totalSessions(((Number) result[7]).intValue())
                    .build();
            responses.add(response);
        }

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