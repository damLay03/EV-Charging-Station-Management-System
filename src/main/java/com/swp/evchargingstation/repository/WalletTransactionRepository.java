package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.entity.WalletTransaction;
import com.swp.evchargingstation.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletOrderByTimestampDesc(Wallet wallet);
    List<WalletTransaction> findByWalletIdOrderByTimestampDesc(Long walletId);
    Optional<WalletTransaction> findByExternalTransactionIdAndStatus(String externalTransactionId, TransactionStatus status);
}

