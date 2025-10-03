package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, String> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Plan> findByNameIgnoreCase(String name);
}

