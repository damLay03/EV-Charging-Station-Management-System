package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.PaymentStatus;
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
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "payment_id")
    String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    Driver payer;

    @Column(name = "amount")
    float amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    PaymentMethod method;

    @Column(name = "payment_time")
    LocalDateTime paymentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    PaymentStatus status;

    @Column(name = "txn_reference")
    String txnReference;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    ChargingSession session;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    Subscription subscription;
}
