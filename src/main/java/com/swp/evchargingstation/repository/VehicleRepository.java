package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.enums.VehicleRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByOwner_UserId(String ownerId);
    List<Vehicle> findByOwner_UserIdAndApprovalStatus(String ownerId, VehicleRegistrationStatus status);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    boolean existsByLicensePlate(String licensePlate);
    List<Vehicle> findByApprovalStatus(VehicleRegistrationStatus status);
    List<Vehicle> findByApprovalStatusOrderBySubmittedAtDesc(VehicleRegistrationStatus status);
}

