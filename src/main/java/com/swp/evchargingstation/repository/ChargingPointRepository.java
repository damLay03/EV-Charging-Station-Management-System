package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargingPointRepository extends JpaRepository<ChargingPoint, String> {
    @Query("SELECT COUNT(cp) FROM ChargingPoint cp WHERE cp.station.stationId = :stationId")
    Integer countByStationId(@Param("stationId") String stationId);

    @Query("SELECT COUNT(cp) FROM ChargingPoint cp WHERE cp.station.stationId = :stationId AND cp.status = :status")
    Integer countByStationIdAndStatus(@Param("stationId") String stationId, @Param("status") ChargingPointStatus status);

    List<ChargingPoint> findByStation_StationId(String stationId);

    // Đếm số lượng charging point theo status
    long countByStatus(ChargingPointStatus status);
}
