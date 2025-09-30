package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.PaymentMethodType;
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
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "pm_id")
    String pmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type")
    PaymentMethodType methodType;

    @Column(name = "provider")
    String provider;

    @Column(name = "token")
    String token;
}
