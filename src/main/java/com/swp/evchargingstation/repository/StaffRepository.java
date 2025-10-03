package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    List<Staff> findByStation_StationId(String stationId);
    List<Staff> findByStationIsNull();
}

