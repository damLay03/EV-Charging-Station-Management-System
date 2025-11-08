package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.TopUpCashRequest;
import com.swp.evchargingstation.dto.request.TopUpZaloPayRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.TopUpZaloPayResponse;
import com.swp.evchargingstation.dto.response.WalletBalanceResponse;
import com.swp.evchargingstation.dto.response.WalletTransactionResponse;
import com.swp.evchargingstation.entity.WalletTransaction;
import com.swp.evchargingstation.service.TopUpService;
import com.swp.evchargingstation.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;
    private final TopUpService topUpService;

    /**
     * Get current wallet balance
     * Accessible by DRIVER only
     */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<WalletBalanceResponse> getBalance(Authentication authentication) {
        String userId = authentication.getName();
        WalletBalanceResponse response = walletService.getWalletBalance(userId);
        return ApiResponse.<WalletBalanceResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Get wallet transaction history
     * Accessible by DRIVER only
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<WalletTransactionResponse>> getHistory(Authentication authentication) {
        String userId = authentication.getName();
        List<WalletTransactionResponse> history = walletService.getTransactionHistory(userId);
        return ApiResponse.<List<WalletTransactionResponse>>builder()
                .result(history)
                .build();
    }

    /**
     * Create ZaloPay top-up order
     * Accessible by DRIVER only
     */
    @PostMapping("/topup/zalopay")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<TopUpZaloPayResponse> createZaloPayTopUp(
            Authentication authentication,
            @Valid @RequestBody TopUpZaloPayRequest request) {
        String userId = authentication.getName();
        log.info("Creating ZaloPay top-up for user: {}", userId);
        TopUpZaloPayResponse response = topUpService.createZaloPayTopUp(userId, request);
        return ApiResponse.<TopUpZaloPayResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Process cash top-up for a user
     * Accessible by STAFF only
     */
    @PostMapping("/topup/cash")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<WalletTransactionResponse> processCashTopUp(
            Authentication authentication,
            @Valid @RequestBody TopUpCashRequest request) {
        String staffId = authentication.getName();
        log.info("Processing cash top-up by staff: {}", staffId);
        WalletTransaction transaction = topUpService.processCashTopUp(staffId, request);

        // Map to response
        WalletTransactionResponse response = WalletTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .description(transaction.getDescription())
                .processedByStaffId(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUserId() : null)
                .processedByStaffName(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUser().getFullName() : null)
                .build();

        return ApiResponse.<WalletTransactionResponse>builder()
                .result(response)
                .build();
    }
}

