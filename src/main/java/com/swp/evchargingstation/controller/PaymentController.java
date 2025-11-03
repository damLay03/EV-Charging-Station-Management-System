package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.CashPaymentRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.service.CashPaymentService;
import com.swp.evchargingstation.service.ZaloPayService;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCallbackRequest;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Payment", description = "Payment APIs - Cash Payment Only")
public class PaymentController {

    CashPaymentService cashPaymentService;
    ZaloPayService zaloPayService;

    @PostMapping("/cash/request")
    @Operation(
            summary = "Yêu cầu thanh toán tiền mặt",
            description = "Driver yêu cầu thanh toán tiền mặt cho phiên sạc. Yêu cầu sẽ được gửi tới nhân viên trạm sạc để xác nhận"
    )
    public ApiResponse<String> requestCashPayment(@RequestBody @Valid CashPaymentRequest request,
                                                   @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        String driverId = jwt.getClaim("userId");
        log.info("Driver {} requesting cash payment for session {}", driverId, request.getSessionId());

        cashPaymentService.requestCashPayment(request.getSessionId());

        return ApiResponse.<String>builder()
                .message("Yêu cầu thanh toán tiền mặt đã được gửi đến nhân viên trạm")
                .result("PENDING")
                .build();
    }

    /**
     * Create ZaloPay payment
     */
    @PostMapping("/sessions/{sessionId}/zalopay")
    @Operation(
            summary = "Tạo thanh toán ZaloPay",
            description = "Tạo một đơn thanh toán ZaloPay cho phiên sạc. Trả về URL thanh toán để driver chuyển hướng sang cổng thanh toán ZaloPay"
    )
    public ApiResponse<String> createZaloPayPayment(@PathVariable String sessionId) {
        String paymentUrl = zaloPayService.createPayment(sessionId);
        return ApiResponse.<String>builder()
                .result(paymentUrl)
                .build();
    }

    /**
     * ZaloPay callback endpoint
     */
    @PostMapping("/callbacks/zalopay")
    @Operation(
            summary = "Xử lý callback từ ZaloPay",
            description = "Endpoint nhận callback từ ZaloPay để xác nhận kết quả thanh toán. ZaloPay sẽ gửi thông tin thanh toán tới đây sau khi người dùng hoàn tất giao dịch"
    )
    public ResponseEntity<Map<String, Object>> zaloPayCallback(
            @RequestBody ZaloPayCallbackRequest callbackRequest
    ) {
        log.info("=== ZaloPay Callback Received ===");
        log.info("Data: {}", callbackRequest.getData());
        log.info("MAC: {}", callbackRequest.getMac());

        Map<String, Object> response = zaloPayService.handleCallback(callbackRequest);

        log.info("Callback response: {}", response);
        log.info("=== End ZaloPay Callback ===");

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify callback URL is accessible
     */
    @GetMapping("/callbacks/zalopay/test")
    @Operation(
            summary = "Kiểm tra callback endpoint của ZaloPay",
            description = "Endpoint kiểm tra xem URL callback ZaloPay có hoạt động bình thường không. Dùng để xác minh rằng ZaloPay có thể kết nối tới hệ thống"
    )
    public ResponseEntity<String> testCallback() {
        log.info("ZaloPay callback test endpoint hit!");
        return ResponseEntity.ok("ZaloPay callback endpoint is accessible. POST to this URL for actual callback.");
    }
}
