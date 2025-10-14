package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, String> {
    @Query("SELECT COUNT(cs) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Integer countByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Double sumTotalSpentByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COALESCE(SUM(cs.energyKwh), 0) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Double sumTotalEnergyByDriverId(@Param("driverId") String driverId);

    @Query("SELECT cs FROM ChargingSession cs WHERE cs.driver.userId = :driverId ORDER BY cs.startTime DESC")
    List<ChargingSession> findByDriverIdOrderByStartTimeDesc(@Param("driverId") String driverId);

    @Query("SELECT cs.endSocPercent FROM ChargingSession cs WHERE cs.driver.userId = :driverId AND cs.endSocPercent IS NOT NULL ORDER BY cs.endTime DESC LIMIT 1")
    java.util.Optional<Integer> findLatestEndSocByDriverId(@Param("driverId") String driverId);

    // ========== ANALYTICS QUERIES FOR DRIVER ==========

    /**
     * Lấy danh sách sessions theo tháng và năm của driver
     */
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND YEAR(cs.startTime) = :year " +
            "AND MONTH(cs.startTime) = :month")
    List<ChargingSession> findByDriverIdAndYearAndMonth(
            @Param("driverId") String driverId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * Tính tổng chi phí theo tháng của driver
     */
    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND YEAR(cs.startTime) = :year " +
            "AND MONTH(cs.startTime) = :month")
    Double sumCostByDriverAndMonth(
            @Param("driverId") String driverId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * Tính tổng năng lượng theo tháng của driver
     */
    @Query("SELECT COALESCE(SUM(cs.energyKwh), 0) FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND YEAR(cs.startTime) = :year " +
            "AND MONTH(cs.startTime) = :month")
    Double sumEnergyByDriverAndMonth(
            @Param("driverId") String driverId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * Đếm số phiên sạc theo tháng của driver
     */
    @Query("SELECT COUNT(cs) FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND YEAR(cs.startTime) = :year " +
            "AND MONTH(cs.startTime) = :month")
    Integer countSessionsByDriverAndMonth(
            @Param("driverId") String driverId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs WHERE cs.chargingPoint.station.stationId = :stationId")
    Double sumRevenueByStationId(@Param("stationId") String stationId);

    // ========== NEW QUERIES FOR USAGE ANALYTICS ==========

    /**
     * Đếm số session của một trạm trong khoảng thời gian
     */
    @Query("SELECT COUNT(cs) FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.startTime >= :startTime " +
            "AND cs.startTime < :endTime")
    Integer countSessionsByStationAndTimeRange(
            @Param("stationId") String stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Đếm số session của một trạm theo trạng thái trong khoảng thời gian
     */
    @Query("SELECT COUNT(cs) FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.status = :status " +
            "AND cs.startTime >= :startTime " +
            "AND cs.startTime < :endTime")
    Integer countSessionsByStationStatusAndTimeRange(
            @Param("stationId") String stationId,
            @Param("status") ChargingSessionStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Tính tổng năng lượng sạc của một trạm trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(cs.energyKwh), 0) FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.startTime >= :startTime " +
            "AND cs.startTime < :endTime")
    Double sumEnergyByStationAndTimeRange(
            @Param("stationId") String stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Tính tổng doanh thu của một trạm trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.startTime >= :startTime " +
            "AND cs.startTime < :endTime")
    Double sumRevenueByStationAndTimeRange(
            @Param("stationId") String stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Thống kê theo giờ cho một trạm trong khoảng thời gian
     * Return: [giờ, số phiên, tổng năng lượng, tổng doanh thu]
     */
    @Query("SELECT HOUR(cs.startTime) as hour, " +
           "COUNT(cs) as sessions, " +
           "SUM(cs.energyKwh) as energy, " +
           "SUM(cs.costTotal) as revenue " +
           "FROM ChargingSession cs " +
           "WHERE cs.chargingPoint.station.stationId = :stationId " +
           "AND cs.startTime >= :startTime " +
           "AND cs.startTime < :endTime " +
           "GROUP BY HOUR(cs.startTime) " +
           "ORDER BY hour")
    List<Object[]> findHourlyUsageByStation(
            @Param("stationId") String stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ========== DASHBOARD QUERIES ==========

    /**
     * Tính tổng theo khoảng thời gian cho dashboard summary
     */
    @Query("SELECT COALESCE(SUM(cs.costTotal), 0), COALESCE(SUM(cs.energyKwh), 0), COUNT(cs) " +
            "FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND cs.startTime >= :startTime")
    Object[] getSummaryByDriverAndStartTime(
            @Param("driverId") String driverId,
            @Param("startTime") LocalDateTime startTime
    );

    /**
     * Thống kê theo giờ trong ngày
     */
    @Query("SELECT HOUR(cs.startTime), COUNT(cs), COALESCE(SUM(cs.energyKwh), 0) " +
            "FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND DATE(cs.startTime) = :date " +
            "GROUP BY HOUR(cs.startTime) " +
            "ORDER BY HOUR(cs.startTime)")
    List<Object[]> getHourlySessionsByDriverAndDate(
            @Param("driverId") String driverId,
            @Param("date") LocalDate date
    );

    /**
     * Lấy top trạm sạc yêu thích
     */
    @Query("SELECT s.stationId, s.name, s.address, COUNT(cs) " +
            "FROM ChargingSession cs " +
            "JOIN cs.chargingPoint cp " +
            "JOIN cp.station s " +
            "WHERE cs.driver.userId = :driverId " +
            "GROUP BY s.stationId, s.name, s.address " +
            "ORDER BY COUNT(cs) DESC")
    List<Object[]> getFavoriteStationsByDriver(@Param("driverId") String driverId);

    /**
     * Tính thời gian sạc trung bình
     */
    @Query("SELECT AVG(cs.durationMin) " +
            "FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "AND cs.durationMin > 0")
    Double getAverageChargingTimeByDriver(@Param("driverId") String driverId);

    /**
     * Thống kê theo giờ để tìm giờ cao điểm
     */
    @Query("SELECT HOUR(cs.startTime), COUNT(cs) " +
            "FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "GROUP BY HOUR(cs.startTime) " +
            "ORDER BY COUNT(cs) DESC")
    List<Object[]> getPeakHoursByDriver(@Param("driverId") String driverId);

    /**
     * Thống kê theo ngày trong tuần
     */
    @Query("SELECT DAYOFWEEK(cs.startTime), COUNT(cs) " +
            "FROM ChargingSession cs " +
            "WHERE cs.driver.userId = :driverId " +
            "GROUP BY DAYOFWEEK(cs.startTime) " +
            "ORDER BY COUNT(cs) DESC")
    List<Object[]> getMostFrequentDaysByDriver(@Param("driverId") String driverId);
}
