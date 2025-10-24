package com.swp.evchargingstation.entity;

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

    // Helper methods để lấy thông tin từ model
    public float getBatteryCapacityKwh() {
        return model != null ? model.getBatteryCapacityKwh() : 0f;
    }

    public String getBatteryType() {
        return model != null ? model.getBatteryType() : null;
    }

    public com.swp.evchargingstation.enums.VehicleBrand getBrand() {
        return model != null ? model.getBrand() : null;
    }
}
