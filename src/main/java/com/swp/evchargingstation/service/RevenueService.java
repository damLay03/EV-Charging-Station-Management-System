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