package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Incident;
import com.swp.evchargingstation.enums.IncidentStatus;
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

    // Tìm incidents theo station và status (nếu cần sau này thì dùng @Query)
    @Query("SELECT i FROM Incident i WHERE i.station.stationId = :stationId AND i.status = :status ORDER BY i.reportedAt DESC")
    List<Incident> findByStation_StationIdAndStatus(@Param("stationId") String stationId, @Param("status") IncidentStatus status);

    // Tìm tất cả incidents của một station, sắp xếp theo thời gian
    @Query("SELECT i FROM Incident i " +
           "LEFT JOIN FETCH i.reporter " +
           "LEFT JOIN FETCH i.station " +
           "LEFT JOIN FETCH i.chargingPoint " +
           "LEFT JOIN FETCH i.assignedStaff s " +
           "LEFT JOIN FETCH s.user " +
           "WHERE i.station.stationId = :stationId " +
           "ORDER BY i.reportedAt DESC")
    List<Incident> findByStationIdOrderByReportedAtDesc(@Param("stationId") String stationId);

    // Tìm TẤT CẢ incidents (dành cho ADMIN)
    @Query("SELECT i FROM Incident i " +
           "LEFT JOIN FETCH i.reporter " +
           "LEFT JOIN FETCH i.station " +
           "LEFT JOIN FETCH i.chargingPoint " +
           "LEFT JOIN FETCH i.assignedStaff s " +
           "LEFT JOIN FETCH s.user " +
           "ORDER BY i.reportedAt DESC")
    List<Incident> findAllByOrderByReportedAtDesc();
}
