package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.driver.userId = :driverId")
    List<PaymentMethod> findAllByDriverId(@Param("driverId") String driverId);
}

