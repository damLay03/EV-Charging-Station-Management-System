package com.swp.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "wallets")
public class Wallet {
    @Id
    @Column(name = "wallet_id", length = 36, nullable = false)
    String walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    Double balance = 0.0;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // Generate UUID if not set
        if (this.walletId == null) {
            this.walletId = UUID.randomUUID().toString();
        }
        // Set timestamp
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

