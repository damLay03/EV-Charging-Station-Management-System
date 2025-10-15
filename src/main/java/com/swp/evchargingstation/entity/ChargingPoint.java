package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.ChargingPointStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    @Column(name = "max_power_kw")
    float maxPowerKw;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ChargingPointStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_session_id")
    ChargingSession currentSession;
}
