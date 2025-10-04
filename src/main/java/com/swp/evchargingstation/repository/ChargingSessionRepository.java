package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, String> {
    @Query("SELECT COUNT(cs) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Integer countByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COALESCE(SUM(cs.costTotal), 0) FROM ChargingSession cs WHERE cs.driver.userId = :driverId")
    Double sumTotalSpentByDriverId(@Param("driverId") String driverId);
}
