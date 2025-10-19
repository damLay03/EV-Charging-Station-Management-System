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

    // Một staff chỉ quản lý một station
    @OneToOne(mappedBy = "staff", fetch = FetchType.LAZY)
    Station station;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;
}
