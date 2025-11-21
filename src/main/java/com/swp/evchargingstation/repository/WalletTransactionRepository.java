package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.entity.WalletTransaction;
import com.swp.evchargingstation.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletOrderByTimestampDesc(Wallet wallet);

    // Query by wallet's UUID using underscore notation to navigate nested property
    List<WalletTransaction> findByWallet_WalletIdOrderByTimestampDesc(String walletId);

    Optional<WalletTransaction> findByExternalTransactionIdAndStatus(String externalTransactionId, TransactionStatus status);
    List<WalletTransaction> findByWalletAndTimestampBetween(Wallet wallet, LocalDateTime start, LocalDateTime end);
}
