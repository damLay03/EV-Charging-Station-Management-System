package com.swp.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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

    // Một staff có thể quản lý nhiều station
    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    List<Station> stations;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;
}
