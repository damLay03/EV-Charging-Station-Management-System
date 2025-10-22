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
    @Column(name = "brand", nullable = false)
    VehicleBrand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "model", nullable = false)
    VehicleModel model;

    @Column(name = "battery_capacity_kwh")
    float batteryCapacityKwh;

    @Column(name = "battery_type")
    String batteryType;

    @Column(name = "current_soc_percent")
    Integer currentSocPercent; // % pin hiện tại của xe - ĐÃ SỬA int → Integer (FIX LỖI)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    Driver owner;
}
