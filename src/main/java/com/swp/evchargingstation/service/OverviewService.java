package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.SystemOverviewResponse;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.PaymentRepository;
import com.swp.evchargingstation.repository.StationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OverviewService {

    StationRepository stationRepository;
    ChargingPointRepository chargingPointRepository;
    DriverRepository driverRepository;
    PaymentRepository paymentRepository;

    public SystemOverviewResponse getSystemOverview() {
        log.info("Fetching system overview data");

        // 1. Tổng số trạm sạc
        long totalStations = getTotalStations();
        log.info("Total stations: {}", totalStations);

        // 2. Điểm sạc đang hoạt động (đang được cắm sạc - status = OCCUPIED)
        long activeChargingPoints = chargingPointRepository.countByStatus(ChargingPointStatus.OCCUPIED);
        log.info("Active charging points: {}", activeChargingPoints);

        // 3. Tổng số người dùng (driver)
        long totalDrivers = getTotalDrivers();
        log.info("Total drivers: {}", totalDrivers);

        // 4. Doanh thu tháng hiện tại (từ ngày 1 đến hiện tại)
        LocalDate now = LocalDate.now();
        Float currentMonthRevenue = paymentRepository.findCurrentMonthRevenue(
                now.getYear(),
                now.getMonthValue()
        );
        // Nếu chưa có doanh thu thì trả về 0
        float revenue = currentMonthRevenue != null ? currentMonthRevenue : 0f;
        log.info("Current month revenue (Year: {}, Month: {}): {}", now.getYear(), now.getMonthValue(), revenue);

        return SystemOverviewResponse.builder()
                .totalStations(totalStations)
                .activeChargingPoints(activeChargingPoints)
                .totalDrivers(totalDrivers)
                .currentMonthRevenue(revenue)
                .build();
    }

    /**
     * Tính tổng số trạm sạc trong hệ thống
     *
     * @return Tổng số trạm sạc
     */
    public long getTotalStations() {
        log.info("Calculating total stations");
        long totalStations = stationRepository.count();
        log.info("Total stations count: {}", totalStations);
        return totalStations;
    }

    /**
     * Tính tổng số driver trong hệ thống
     *
     * @return Tổng số driver
     */
    public long getTotalDrivers() {
        log.info("Calculating total drivers");
        long totalDrivers = driverRepository.count();
        log.info("Total drivers count: {}", totalDrivers);
        return totalDrivers;
    }
}