package com.swp.evchargingstation.entity;

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
@Table(name = "drivers")
public class Driver {
    @Id
    @Column(name = "user_id")
    String userId;

    @Column(name = "address")
    String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    User user;


}
