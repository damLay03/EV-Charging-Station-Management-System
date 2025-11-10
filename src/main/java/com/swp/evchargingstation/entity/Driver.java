package com.swp.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "drivers")
public class Driver {
    @Id
    @Column(name = "user_id")
    String userId;

    @Column(name = "address")
    String address;

    @Column(name = "join_date")
    LocalDateTime joinDate; // Mốc thời gian đăng ký

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    Plan plan; // Gói plan hiện tại của driver

    @Column(name = "plan_subscription_date")
    LocalDateTime planSubscriptionDate; // Ngày đăng ký plan (dùng cho auto renew)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;

    // One Driver can own many Vehicles
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    List<Vehicle> vehicles = new ArrayList<>();
}
