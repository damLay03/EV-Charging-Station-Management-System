package com.swp.evchargingstation.entity;

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

    @Column(name = "model")
    String model;

    @Column(name = "battery_capacity_kwh")
    float batteryCapacityKwh;

    @Column(name = "battery_type")
    String batteryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    Driver owner;
}
