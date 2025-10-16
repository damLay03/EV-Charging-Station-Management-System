package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    // Staff có List<Station> stations, không có single Station
    // Không cần query theo station vì mối quan hệ ngược lại: Station -> Staff
}
