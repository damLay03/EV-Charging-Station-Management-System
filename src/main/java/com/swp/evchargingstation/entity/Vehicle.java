package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.enums.VehicleModel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "vehicle_id")
    String vehicleId;

    @Column(name = "license_plate", unique = true)
    String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "model", nullable = false)
    VehicleModel model;

    @Column(name = "current_soc_percent")
    Integer currentSocPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    Driver owner;

    // Thêm các field để lưu vào database
    @Column(name = "battery_capacity_kwh")
    Float batteryCapacityKwhValue;

    @Column(name = "battery_type")
    String batteryTypeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand")
    VehicleBrand brandValue;

    @Column(name = "max_charging_power")
    String maxChargingPowerValue;

    @Column(name = "max_charging_power_kw")
    Float maxChargingPowerKwValue;

    // Helper methods để lấy thông tin (ưu tiên từ DB, fallback về model)
    public float getBatteryCapacityKwh() {
        if (batteryCapacityKwhValue != null) {
            return batteryCapacityKwhValue;
        }
        return model != null ? model.getBatteryCapacityKwh() : 0f;
    }

    public String getBatteryType() {
        if (batteryTypeValue != null) {
            return batteryTypeValue;
        }
        return model != null ? model.getBatteryType() : null;
    }

    public VehicleBrand getBrand() {
        if (brandValue != null) {
            return brandValue;
        }
        return model != null ? model.getBrand() : null;
    }

    public String getMaxChargingPower() {
        if (maxChargingPowerValue != null) {
            return maxChargingPowerValue;
        }
        return model != null ? model.getMaxChargingPower() : null;
    }

    public float getMaxChargingPowerKw() {
        if (maxChargingPowerKwValue != null) {
            return maxChargingPowerKwValue;
        }
        return model != null ? model.getMaxChargingPowerKw() : 0f;
    }
}
