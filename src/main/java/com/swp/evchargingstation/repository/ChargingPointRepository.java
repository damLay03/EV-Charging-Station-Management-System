package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingPointRepository extends JpaRepository<ChargingPoint, String> {
    // Đếm số điểm sạc theo trạng thái
    long countByStatus(ChargingPointStatus status);
}