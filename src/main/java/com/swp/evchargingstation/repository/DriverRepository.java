package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    Optional<Driver> findByUserId(String userId);

    @Query("SELECT d FROM Driver d JOIN FETCH d.user")
    List<Driver> findAllWithUser();

    @Query("SELECT d FROM Driver d LEFT JOIN FETCH d.plan WHERE d.userId = :userId")
    Optional<Driver> findByUserIdWithPlan(String userId);
}
