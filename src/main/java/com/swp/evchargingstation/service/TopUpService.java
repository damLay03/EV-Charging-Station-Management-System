package com.swp.evchargingstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp.evchargingstation.configuration.ZaloPayConfig;
import com.swp.evchargingstation.dto.request.TopUpCashRequest;
import com.swp.evchargingstation.dto.request.TopUpZaloPayRequest;
import com.swp.evchargingstation.dto.response.TopUpZaloPayResponse;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCallbackRequest;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCreateRequest;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCreateResponse;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.entity.WalletTransaction;
import com.swp.evchargingstation.enums.TransactionStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.StaffRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.repository.WalletRepository;
import com.swp.evchargingstation.repository.WalletTransactionRepository;
import com.swp.evchargingstation.util.ZaloPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopUpService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ZaloPayConfig zaloPayConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create ZaloPay top-up order for a user
     */
    @Transactional
    public TopUpZaloPayResponse createZaloPayTopUp(String userId, TopUpZaloPayRequest request) {
        log.info("Creating ZaloPay top-up - userId: {}, amount: {}", userId, request.getAmount());

        Double amount = request.getAmount();

        if (amount <= 0) {
            log.error("Invalid top-up amount: {}", amount);
            throw new AppException(ErrorCode.INVALID_TOPUP_AMOUNT);
        }

        // Find user by userId
        log.debug("Looking for user with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with userId: {}", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        log.info("Found user: {} ({})", user.getEmail(), user.getUserId());

        // Ensure wallet exists
        try {
            walletService.getWallet(userId);
            log.debug("Wallet exists for user: {}", userId);
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.WALLET_NOT_FOUND) {
                log.info("Creating wallet for user: {}", userId);
                walletService.createWalletByUserId(userId);
            } else {
                throw e;
            }
        }

        // Generate unique transaction ID
        String appTransId = ZaloPayUtil.generateAppTransId();
        long appTime = System.currentTimeMillis();

        // Create embed_data
        Map<String, Object> embedDataMap = new HashMap<>();
        embedDataMap.put("user_id", userId);
        embedDataMap.put("topup", true);
        embedDataMap.put("redirecturl", zaloPayConfig.getRedirectUrl());
        String embedData = ZaloPayUtil.toEmbedData(embedDataMap);

        // Create item JSON
        String item = ZaloPayUtil.toItemJson(
            "Top-up wallet",
            amount.longValue()
        );

        // Generate MAC
        String mac = ZaloPayUtil.generateMac(
            Integer.parseInt(zaloPayConfig.getAppId()),
            appTransId,
            userId,
            amount.longValue(),
            appTime,
            embedData,
            item,
            zaloPayConfig.getKey1()
        );

        // Build ZaloPay request
        ZaloPayCreateRequest zaloPayRequest = ZaloPayCreateRequest.builder()
                .appId(Integer.parseInt(zaloPayConfig.getAppId()))
                .appTransId(appTransId)
                .appUser(userId)
                .appTime(appTime)
                .amount(amount.longValue())
                .embedData(embedData)
                .item(item)
                .description("Nạp tiền ví cho người dùng " + userId)
                .bankCode("")
                .mac(mac)
                .callbackUrl(zaloPayConfig.getCallbackUrl() + "/topup")  // Use separate topup callback
                .build();

        log.info("Creating ZaloPay top-up for user: {}, amount: {}", userId, amount);

        // Call ZaloPay API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ZaloPayCreateRequest> entity = new HttpEntity<>(zaloPayRequest, headers);

        ResponseEntity<ZaloPayCreateResponse> response = restTemplate.exchange(
                zaloPayConfig.getEndpoint(),
                HttpMethod.POST,
                entity,
                ZaloPayCreateResponse.class
        );

        ZaloPayCreateResponse zaloPayResponse = response.getBody();

        if (zaloPayResponse == null || zaloPayResponse.getReturnCode() != 1) {
            log.error("ZaloPay API error: {}", zaloPayResponse);
            throw new AppException(ErrorCode.ZALOPAY_API_ERROR);
        }

        // Create pending wallet transaction
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(walletService.getWallet(userId))
                .amount(amount)
                .transactionType(TransactionType.TOPUP_ZALOPAY)
                .status(TransactionStatus.PENDING)
                .timestamp(LocalDateTime.now())
                .description("Nạp tiền qua ZaloPay")
                .externalTransactionId(appTransId)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("ZaloPay top-up order created. TransactionId: {}, Token: {}",
                transaction.getId(), zaloPayResponse.getZpTransToken());

        return TopUpZaloPayResponse.builder()
                .orderUrl(zaloPayResponse.getOrderUrl())
                .appTransId(appTransId)
                .transactionId(transaction.getId())
                .message("Tạo đơn nạp tiền thành công")
                .build();
    }

    /**
     * Handle ZaloPay callback for top-up
     */
    @Transactional
    public Map<String, Object> handleZaloPayCallback(ZaloPayCallbackRequest callbackRequest) {
        log.info("Received ZaloPay top-up callback");

        // Verify MAC
        boolean isValid = ZaloPayUtil.verifyCallbackMac(
                callbackRequest.getData(),
                callbackRequest.getMac(),
                zaloPayConfig.getKey2()
        );

        if (!isValid) {
            log.error("Invalid callback MAC");
            return createCallbackResponse(-1, "Invalid MAC");
        }

        try {
            // Parse callback data
            Map<String, Object> dataMap = objectMapper.readValue(
                    callbackRequest.getData(),
                    Map.class
            );

            String appTransId = (String) dataMap.get("app_trans_id");
            Number amountNum = (Number) dataMap.get("amount");
            long amount = amountNum.longValue();

            log.info("Processing top-up callback for transaction: {}, amount: {}",
                    appTransId, amount);

            // Find pending transaction
            WalletTransaction transaction = transactionRepository
                    .findByExternalTransactionIdAndStatus(appTransId, TransactionStatus.PENDING)
                    .orElseThrow(() -> {
                        log.error("Transaction not found or not pending: {}", appTransId);
                        return new AppException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND);
                    });

            // Update wallet balance
            Wallet wallet = transaction.getWallet();
            wallet.setBalance(wallet.getBalance() + amount);
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);

            log.info("Updated wallet balance for user: {}. New balance: {}",
                    wallet.getUser().getUserId(), wallet.getBalance());

            // Update transaction status to COMPLETED
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            log.info("Top-up completed successfully: {}. Amount: {}, User: {}",
                    appTransId, amount, wallet.getUser().getUserId());

            return createCallbackResponse(1, "Success");

        } catch (Exception e) {
            log.error("Error processing top-up callback", e);
            return createCallbackResponse(-1, "Error: " + e.getMessage());
        }
    }

    /**
     * Process cash top-up by staff
     */
    @Transactional
    public WalletTransaction processCashTopUp(String processingStaffId, TopUpCashRequest request) {
        // Find staff
        Staff staff = staffRepository.findById(processingStaffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Find target user by email or phone
        User targetUser = userRepository.findByEmail(request.getTargetUserIdentifier())
                .or(() -> userRepository.findByPhone(request.getTargetUserIdentifier()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Double amount = request.getAmount();

        if (amount <= 0) {
            throw new AppException(ErrorCode.INVALID_TOPUP_AMOUNT);
        }

        // Ensure wallet exists
        try {
            walletService.getWallet(targetUser.getUserId());
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.WALLET_NOT_FOUND) {
                walletService.createWalletByUserId(targetUser.getUserId());
            } else {
                throw e;
            }
        }

        String description = request.getDescription() != null ?
                request.getDescription() : "Cash top-up by staff";

        // Credit the wallet
        WalletTransaction transaction = walletService.credit(
                targetUser.getUserId(),
                amount,
                TransactionType.TOPUP_CASH,
                description,
                null,
                staff,
                null,
                null
        );

        log.info("Cash top-up completed. Staff: {}, User: {}, Amount: {}",
                processingStaffId, targetUser.getUserId(), amount);

        return transaction;
    }

    /**
     * Create callback response
     */
    private Map<String, Object> createCallbackResponse(int returnCode, String returnMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("return_code", returnCode);
        response.put("return_message", returnMessage);
        return response;
    }
}

