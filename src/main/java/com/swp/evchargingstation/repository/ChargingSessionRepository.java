package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, String> {
    @Query("SELECT COUNT(cs) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Integer countByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Double sumTotalSpentByDriverId(@Param("driverId") String driverId);

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
     * Đếm session theo status và thời gian
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
     * Tính tổng năng lượng (kWh) trong khoảng thời gian
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
     * Tính tổng doanh thu trong khoảng thời gian
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
     * Lấy danh sách session đang IN_PROGRESS của một trạm
     */
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.status = 'IN_PROGRESS'")
    List<ChargingSession> findActiveSessionsByStation(@Param("stationId") String stationId);

    /**
     * Phân tích usage theo giờ - trả về data thô để xử lý
     */
    @Query("SELECT HOUR(cs.startTime), COUNT(cs), SUM(cs.energyKwh), SUM(cs.costTotal) " +
            "FROM ChargingSession cs " +
            "WHERE cs.chargingPoint.station.stationId = :stationId " +
            "AND cs.startTime >= :startTime " +
            "AND cs.startTime < :endTime " +
            "GROUP BY HOUR(cs.startTime) " +
            "ORDER BY HOUR(cs.startTime)")
    List<Object[]> findHourlyUsageByStation(
            @Param("stationId") String stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
