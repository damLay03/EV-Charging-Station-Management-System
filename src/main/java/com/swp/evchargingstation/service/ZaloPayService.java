package com.swp.evchargingstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp.evchargingstation.configuration.ZaloPayConfig;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCallbackRequest;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCreateRequest;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCreateResponse;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.PaymentRepository;
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
public class ZaloPayService {

    private final ZaloPayConfig zaloPayConfig;
    private final ChargingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create ZaloPay payment
     */
    @Transactional
    public String createPayment(String sessionId) {
        // Get charging session
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        // Check if payment already exists for this session
        Payment existingPayment = paymentRepository.findByChargingSession(session).orElse(null);

        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
            }

            // If payment is PENDING, delete it to create a new one
            // This handles the case where user retries payment creation
            log.info("Deleting existing PENDING payment for session: {}", sessionId);
            paymentRepository.delete(existingPayment);
            paymentRepository.flush(); // Force deletion before creating new payment
        }

        // Calculate amount (ZaloPay uses VND, no decimals)
        long amount = (long) session.getCostTotal();

        // Generate unique transaction ID
        String appTransId = ZaloPayUtil.generateAppTransId();
        long appTime = System.currentTimeMillis();

        // Create embed_data (for storing custom data)
        Map<String, Object> embedDataMap = new HashMap<>();
        embedDataMap.put("session_id", sessionId);
        embedDataMap.put("redirecturl", zaloPayConfig.getRedirectUrl());
        String embedData = ZaloPayUtil.toEmbedData(embedDataMap);

        // Create item JSON
        String item = ZaloPayUtil.toItemJson(
            "Charging Session " + sessionId,
            amount
        );

        // Generate MAC
        String mac = ZaloPayUtil.generateMac(
            Integer.parseInt(zaloPayConfig.getAppId()),
            appTransId,
            session.getDriver().getUserId(),
            amount,
            appTime,
            embedData,
            item,
            zaloPayConfig.getKey1()
        );

        // Build request
        ZaloPayCreateRequest request = ZaloPayCreateRequest.builder()
                .appId(Integer.parseInt(zaloPayConfig.getAppId()))
                .appTransId(appTransId)
                .appUser(session.getDriver().getUserId())
                .appTime(appTime)
                .amount(amount)
                .embedData(embedData)
                .item(item)
                .description("Payment for charging session " + sessionId)
                .bankCode("")  // Empty = all payment methods
                .mac(mac)
                .callbackUrl(zaloPayConfig.getCallbackUrl())
                .build();

        log.info("Creating ZaloPay payment for session: {}, amount: {}", sessionId, amount);
        log.debug("Request: {}", request);

        // Call ZaloPay API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ZaloPayCreateRequest> entity = new HttpEntity<>(request, headers);

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

        // Save payment record
        Payment payment = Payment.builder()
                .chargingSession(session)
                .payer(session.getDriver())
                .amount(amount)
                .paymentMethod(Payment.PaymentMethod.ZALOPAY)  // Use enum directly, not .name()
                .transactionId(appTransId)
                .paymentDetails(zaloPayResponse.getZpTransToken())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("ZaloPay payment created successfully. Token: {}", zaloPayResponse.getZpTransToken());

        // Return payment URL
        return zaloPayResponse.getOrderUrl();
    }

    /**
     * Handle ZaloPay callback
     */
    @Transactional
    public Map<String, Object> handleCallback(ZaloPayCallbackRequest callbackRequest) {
        log.info("Received ZaloPay callback");

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
            int amount = (int) dataMap.get("amount");

            log.info("Processing callback for transaction: {}, amount: {}", appTransId, amount);

            // Find payment
            Payment payment = paymentRepository.findByTransactionId(appTransId)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

            // Update payment status
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Payment completed successfully: {}", appTransId);

            return createCallbackResponse(1, "Success");

        } catch (Exception e) {
            log.error("Error processing callback", e);
            return createCallbackResponse(-1, "Error: " + e.getMessage());
        }
    }

    private Map<String, Object> createCallbackResponse(int returnCode, String returnMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("return_code", returnCode);
        response.put("return_message", returnMessage);
        return response;
    }
}