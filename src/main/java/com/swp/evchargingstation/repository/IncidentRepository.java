package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {

    // Tìm incidents theo station
    List<Incident> findByStation_StationId(String stationId);

    // Tìm incidents theo staff được assign
    List<Incident> findByAssignedStaff_UserIdOrderByReportedAtDesc(String staffId);

    // Tìm incidents theo station và status
    @Query("SELECT i FROM Incident i WHERE i.station.stationId = :stationId AND i.status = :status ORDER BY i.reportedAt DESC")
    List<Incident> findByStationIdAndStatus(@Param("stationId") String stationId, @Param("status") String status);

    // Tìm tất cả incidents của một station, sắp xếp theo thời gian
    @Query("SELECT i FROM Incident i WHERE i.station.stationId = :stationId ORDER BY i.reportedAt DESC")
    List<Incident> findByStationIdOrderByReportedAtDesc(@Param("stationId") String stationId);
}

