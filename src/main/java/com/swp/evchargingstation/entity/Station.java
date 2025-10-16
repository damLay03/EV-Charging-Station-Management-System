package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.StationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "stations")
public class Station {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "station_id")
    String stationId;

    @Column(name = "name")
    String name;

    @Column(name = "address")
    String address;

    @Column(name = "operator_name")
    String operatorName;

    @Column(name = "contact_phone")
    String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    StationStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", unique = true)
    User staff;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL)
    List<ChargingPoint> chargingPoints;
}
