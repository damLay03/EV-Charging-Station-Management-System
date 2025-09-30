package com.swp.evchargingstation.entity;

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
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "incident_id")
    String incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id")
    ChargingPoint chargingPoint;

    @Column(name = "reported_at")
    LocalDateTime reportedAt;

    @Column(name = "description")
    String description;

    @Column(name = "severity")
    String severity;

    @Column(name = "status")
    String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    Staff assignedStaff;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;
}
