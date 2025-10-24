package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.ChargingPower;
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
@Table(name = "charging_points")
public class ChargingPoint {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "point_id")
    String pointId;

    @Column(name = "name")
    String name; // Ví dụ: TS1, TS2, ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_power_kw")
    ChargingPower chargingPower;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ChargingPointStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_session_id")
    ChargingSession currentSession;
}
