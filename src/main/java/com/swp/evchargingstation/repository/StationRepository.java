package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Station;
import com.swp.evchargingstation.enums.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, String> {
    List<Station> findByStatus(StationStatus status);

}
