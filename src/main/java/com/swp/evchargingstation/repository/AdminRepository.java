package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Admin;
import com.swp.evchargingstation.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, String> {
}
