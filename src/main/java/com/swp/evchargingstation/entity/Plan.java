package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.BillingType;
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
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "plan_id")
    String planId;

    @Column(name = "name")
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type")
    BillingType billingType;

    @Column(name = "price_per_kwh")
    float pricePerKwh;

    @Column(name = "price_per_minute")
    float pricePerMinute;

    @Column(name = "monthly_fee")
    float monthlyFee;

    @Column(name = "benefits")
    String benefits;
}
