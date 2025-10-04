package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    // JpaRepository đã cung cấp sẵn count() method để đếm tổng số driver
}