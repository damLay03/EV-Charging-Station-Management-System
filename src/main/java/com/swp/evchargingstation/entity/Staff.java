package com.swp.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "staffs")
public class Staff {
    @Id
    @Column(name = "user_id")
    String userId;

    @Column(name = "employee_no")
    String employeeNo;

    @Column(name = "position")
    String position;

    // NOTE: Quan hệ nhiều-nhiều tiềm năng (1 station có nhiều staff, 1 staff thuộc 1 station tại 1 thời điểm) -> ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;
}
