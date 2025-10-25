package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.VNPayPaymentRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.VNPayCallbackResponse;
import com.swp.evchargingstation.dto.response.VNPayPaymentResponse;
import com.swp.evchargingstation.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Payment", description = "VNPay Payment APIs")
public class PaymentController {

    VNPayService vnPayService;

    @PostMapping("/vnpay/create")
    @Operation(summary = "Create VNPay payment URL",
               description = "Create payment URL for charging session, driver will be redirected to VNPay")
    public ApiResponse<VNPayPaymentResponse> createPayment(
            @RequestBody VNPayPaymentRequest paymentRequest,
            HttpServletRequest httpRequest) {

        log.info("Creating VNPay payment for session: {}", paymentRequest.getSessionId());

        String paymentUrl = vnPayService.createVnPayPayment(paymentRequest, httpRequest);

        VNPayPaymentResponse response = VNPayPaymentResponse.builder()
                .code("00")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();

        return ApiResponse.<VNPayPaymentResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/vnpay-callback")
    @Operation(summary = "VNPay callback handler",
               description = "Handle callback from VNPay after payment")
    public ApiResponse<VNPayCallbackResponse> handleCallback(
            @RequestParam Map<String, String> params) {

        log.info("Received VNPay callback with response code: {}", params.get("vnp_ResponseCode"));

        // Process callback
        vnPayService.handleVNPayCallback(params);

        // Build response
        String responseCode = params.get("vnp_ResponseCode");
        String sessionId = params.get("vnp_TxnRef");
        String transactionId = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");
        Long amount = amountStr != null ? Long.parseLong(amountStr) / 100 : 0L;

        VNPayCallbackResponse response = VNPayCallbackResponse.builder()
                .code(responseCode)
                .message("00".equals(responseCode) ? "Payment successful" : "Payment failed")
                .sessionId(sessionId)
                .transactionId(transactionId)
                .amount(amount)
                .paymentStatus("00".equals(responseCode) ? "SUCCESS" : "FAILED")
                .bankCode(params.get("vnp_BankCode"))
                .cardType(params.get("vnp_CardType"))
                .payDate(params.get("vnp_PayDate"))
                .build();

        return ApiResponse.<VNPayCallbackResponse>builder()
                .result(response)
                .build();
    }
}
