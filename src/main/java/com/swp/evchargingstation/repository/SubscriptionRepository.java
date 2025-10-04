package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    @Query("SELECT s FROM Subscription s WHERE s.driver.userId = :driverId AND s.status = 'ACTIVE' ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveSubscriptionByDriverId(@Param("driverId") String driverId);
}

