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

    @Column(name = "payment_time")
    LocalDateTime paymentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    PaymentStatus status;

    @Column(name = "txn_reference")
    String txnReference;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    ChargingSession chargingSession;

    // Payment fields
    @Column(name = "transaction_id")
    String transactionId; // Transaction number

//    @Column(name = "payment_method")
//    String paymentMethod; // CASH, etc.

    @Column(name = "payment_details")
    String paymentDetails; // Store additional payment details

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    // Cash payment specific fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    Staff assignedStaff; // Staff được assign xử lý thanh toán tiền mặt

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_staff_id")
    Staff confirmedByStaff; // Staff đã xác nhận thanh toán tiền mặt

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt; // Thời gian staff xác nhận thanh toán tiền mặt

    @Column(name = "zp_trans_token")
    private String zpTransToken;  // ZaloPay transaction token

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    public enum PaymentMethod {
        CASH,
        ZALOPAY  // Add this
    }
}
