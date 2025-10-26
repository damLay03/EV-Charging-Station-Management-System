package com.swp.evchargingstation.configuration;

import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(3) // Chạy sau các backfill khác
@RequiredArgsConstructor
@Slf4j
public class VehicleDataBackfillRunner implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting vehicle data backfill...");

        List<Vehicle> vehicles = vehicleRepository.findAll();
        int updatedCount = 0;

        for (Vehicle vehicle : vehicles) {
            // Chỉ update nếu các field chưa có dữ liệu
            boolean needsUpdate = false;

            if (vehicle.getBatteryCapacityKwhValue() == null && vehicle.getModel() != null) {
                vehicle.setBatteryCapacityKwhValue(vehicle.getModel().getBatteryCapacityKwh());
                needsUpdate = true;
            }

            if (vehicle.getBatteryTypeValue() == null && vehicle.getModel() != null) {
                vehicle.setBatteryTypeValue(vehicle.getModel().getBatteryType());
                needsUpdate = true;
            }

            if (vehicle.getBrandValue() == null && vehicle.getModel() != null) {
                vehicle.setBrandValue(vehicle.getModel().getBrand());
                needsUpdate = true;
            }

            if (needsUpdate) {
                vehicleRepository.save(vehicle);
                updatedCount++;
                log.info("Updated vehicle {} - Brand: {}, Battery: {} kWh, Type: {}",
                    vehicle.getVehicleId(),
                    vehicle.getBrand(),
                    vehicle.getBatteryCapacityKwh(),
                    vehicle.getBatteryType());
            }
        }

        log.info("Vehicle data backfill completed. Updated {} vehicles out of {}",
            updatedCount, vehicles.size());
    }
}

