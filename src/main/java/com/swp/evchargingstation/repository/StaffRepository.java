package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    // Staff có List<Station> stations, không có single Station
    // Không cần query theo station vì mối quan hệ ngược lại: Station -> Staff

    // Fetch staff với station để tránh lazy loading exception
    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.station WHERE s.userId = :userId")
    Optional<Staff> findByIdWithStation(@Param("userId") String userId);
}
