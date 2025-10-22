package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.ChargingSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "charging_sessions")
public class ChargingSession {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "session_id")
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id")
    ChargingPoint chargingPoint;

    @Column(name = "start_time")
    LocalDateTime startTime;

    @Column(name = "end_time")
    LocalDateTime endTime;

    @Column(name = "start_soc_percent")
    int startSocPercent;

    @Column(name = "end_soc_percent")
    int endSocPercent;

    @Column(name = "target_soc_percent")
    Integer targetSocPercent;

    @Column(name = "energy_kwh")
    float energyKwh;

    @Column(name = "duration_min")
    int durationMin;

    @Column(name = "cost_total")
    float costTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_user_id")
    User startedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ChargingSessionStatus status;
}
