package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.WalletBalanceResponse;
import com.swp.evchargingstation.dto.response.WalletDashboardResponse;
import com.swp.evchargingstation.dto.response.WalletTransactionResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.TransactionStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.repository.WalletRepository;
import com.swp.evchargingstation.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Get wallet by userId
     */
    public Wallet getWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    }

    /**
     * Get balance by userId (String)
     */
    public Double getBalance(String userId) {
        Wallet wallet = getWallet(userId);
        return wallet.getBalance();
    }

    /**
     * Get balance by userId (Long) - DEPRECATED
     * @deprecated Use getBalance(String userId) instead
     */
    @Deprecated
    public Double getBalance(Long userId) {
        Wallet wallet = getWallet(String.valueOf(userId));
        return wallet.getBalance();
    }

    /**
     * Get wallet balance response
     */
    public WalletBalanceResponse getWalletBalance(String userId) {
        Wallet wallet = getWallet(userId);
        return WalletBalanceResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUser().getUserId())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt() != null ?
                    wallet.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                .build();
    }

    /**
     * Get transaction history
     */
    public List<WalletTransactionResponse> getTransactionHistory(String userId) {
        Wallet wallet = getWallet(userId);
        List<WalletTransaction> transactions = transactionRepository
                .findByWalletOrderByTimestampDesc(wallet);

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public WalletTransaction credit(Long userId, Double amount, TransactionType type, String description) {
        return credit(String.valueOf(userId), amount, type, description, null, null, null, null);
    }

    /**
     * Credit (add money) to wallet
     */
    @Transactional
    public WalletTransaction credit(String userId, Double amount, TransactionType type,
                                   String description, String externalTransactionId,
                                   Staff processedByStaff, Long relatedBookingId,
                                   String relatedSessionId) {
        if (amount <= 0) {
            throw new AppException(ErrorCode.INVALID_TOPUP_AMOUNT);
        }

        Wallet wallet = getWallet(userId);

        // Update balance
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .status(TransactionStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .description(description)
                .externalTransactionId(externalTransactionId)
                .processedByStaff(processedByStaff)
                .relatedBookingId(relatedBookingId)
                .relatedSessionId(relatedSessionId)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Credited {} to wallet of user {}. New balance: {}",
                amount, userId, wallet.getBalance());

        // Gửi email thông báo nạp tiền thành công (chỉ cho WALLET_TOP_UP)
        if (type == TransactionType.TOPUP_CASH || type == TransactionType.TOPUP_ZALOPAY) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    emailService.sendWalletTopUpSuccessEmail(user, amount, wallet.getBalance());
                }
            } catch (Exception emailEx) {
                log.warn("Failed to send wallet top-up email for user {}: {}", userId, emailEx.getMessage());
            }
        }

        return transaction;
    }

    public WalletTransaction debit(Long userId, Double amount, TransactionType type, String description) {
        return debit(String.valueOf(userId), amount, type, description, null, null);
    }


    /**
     * Debit (subtract money) from wallet
     */
    @Transactional
    public WalletTransaction debit(String userId, Double amount, TransactionType type,
                                  String description, Long relatedBookingId,
                                  String relatedSessionId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = getWallet(userId);

        // Check sufficient funds
        if (wallet.getBalance() < amount) {
            log.warn("Insufficient funds for user {}. Balance: {}, Required: {}",
                    userId, wallet.getBalance(), amount);
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        // Update balance
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record (negative amount for debit)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(-amount)  // Negative for debit
                .transactionType(type)
                .status(TransactionStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .description(description)
                .relatedBookingId(relatedBookingId)
                .relatedSessionId(relatedSessionId)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Debited {} from wallet of user {}. New balance: {}",
                amount, userId, wallet.getBalance());

        return transaction;
    }

    /**
     * Create wallet for a new user (to be called when creating DRIVER)
     */
    @Transactional
    public Wallet createWallet(User user) {
        // Check if wallet already exists
        if (walletRepository.findByUser(user).isPresent()) {
            throw new AppException(ErrorCode.WALLET_ALREADY_EXISTS);
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(0.0)
                .updatedAt(LocalDateTime.now())
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Created wallet for user: {}", user.getUserId());

        return wallet;
    }

    /**
     * Create wallet by userId
     */
    @Transactional
    public Wallet createWalletByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return createWallet(user);
    }

    /**
     * Get wallet dashboard with statistics for current month
     */
    public WalletDashboardResponse getWalletDashboard(String userId) {
        Wallet wallet = getWallet(userId);

        // Get current month transactions
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now();

        List<WalletTransaction> monthlyTransactions = transactionRepository
                .findByWalletAndTimestampBetween(wallet, startOfMonth, endOfMonth);

        // Calculate statistics
        double monthlySpending = monthlyTransactions.stream()
                .filter(t -> t.getAmount() < 0) // Negative amounts are spending
                .mapToDouble(WalletTransaction::getAmount)
                .sum();

        double monthlyTopUp = monthlyTransactions.stream()
                .filter(t -> t.getAmount() > 0 &&
                       (t.getTransactionType() == TransactionType.TOPUP_CASH ||
                        t.getTransactionType() == TransactionType.TOPUP_ZALOPAY))
                .mapToDouble(WalletTransaction::getAmount)
                .sum();

        int transactionCount = monthlyTransactions.size();

        WalletDashboardResponse.WalletStatistics statistics = WalletDashboardResponse.WalletStatistics.builder()
                .monthlySpending(Math.abs(monthlySpending))
                .monthlyTopUp(monthlyTopUp)
                .transactionCount(transactionCount)
                .build();

        return WalletDashboardResponse.builder()
                .currentBalance(wallet.getBalance())
                .statistics(statistics)
                .build();
    }

    /**
     * Map WalletTransaction to Response DTO
     */
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .description(transaction.getDescription())
                .externalTransactionId(transaction.getExternalTransactionId())
                .processedByStaffId(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUserId() : null)
                .processedByStaffName(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUser().getFullName() : null)
                .relatedBookingId(transaction.getRelatedBookingId())
                .relatedSessionId(transaction.getRelatedSessionId())
                .build();
    }

    /**
     * Get transaction history filtered by type
     */
    public List<WalletTransactionResponse> getTransactionHistoryByType(String userId, String typeFilter) {
        Wallet wallet = getWallet(userId);
        List<WalletTransaction> transactions = transactionRepository
                .findByWalletOrderByTimestampDesc(wallet);

        // Filter based on type
        return transactions.stream()
                .filter(t -> matchesTypeFilter(t, typeFilter))
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if transaction matches the filter
     */
    private boolean matchesTypeFilter(WalletTransaction transaction, String typeFilter) {
        if (typeFilter == null || typeFilter.equalsIgnoreCase("ALL")) {
            return true;
        }

        switch (typeFilter.toUpperCase()) {
            case "TOPUP":
            case "NAP_TIEN":
                return transaction.getTransactionType() == TransactionType.TOPUP_CASH ||
                       transaction.getTransactionType() == TransactionType.TOPUP_ZALOPAY;

            case "CHARGING":
            case "SAC_XE":
                return transaction.getTransactionType() == TransactionType.CHARGING_PAYMENT;

            case "REFUND":
            case "HOAN_TIEN":
                return transaction.getTransactionType() == TransactionType.BOOKING_DEPOSIT_REFUND ||
                       transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("refund");

            default:
                return true;
        }
    }
}
