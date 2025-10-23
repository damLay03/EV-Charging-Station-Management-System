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

    // Station mà staff này ĐANG LÀM VIỆC (mỗi staff thuộc một trạm)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    // Station mà staff này QUẢN LÝ (một staff có thể là manager của 1 station)
    @OneToOne(mappedBy = "staff", fetch = FetchType.LAZY)
    Station managedStation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;
}
