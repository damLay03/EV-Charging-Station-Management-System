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
        throw new com.swp.evchargingstation.exception.AppException(
                com.swp.evchargingstation.exception.ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
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