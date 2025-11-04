package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.service.ZaloPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/zalopay")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "ZaloPay Payments", description = "API thanh toán qua ZaloPay")
public class ZaloPayController {

    ZaloPayService zaloPayService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Tạo thanh toán ZaloPay",
            description = "Tạo một đơn thanh toán ZaloPay cho phiên sạc. Trả về URL thanh toán để driver chuyển hướng sang cổng thanh toán ZaloPay"
    )
    public ApiResponse<String> createZaloPayPayment(@RequestParam String sessionId) {
        log.info("Driver creating ZaloPay payment for session: {}", sessionId);
        String paymentUrl = zaloPayService.createPayment(sessionId);
        return ApiResponse.<String>builder()
                .result(paymentUrl)
                .build();
    }
}

//    @GetMapping("/callbacks/zalopay/test")
//    @Operation(
//            summary = "Kiểm tra callback endpoint của ZaloPay",
//            description = "Endpoint kiểm tra xem URL callback ZaloPay có hoạt động bình thường không. Dùng để xác minh rằng ZaloPay có thể kết nối tới hệ thống"
//    )
//    public ResponseEntity<String> testCallback() {
//        log.info("ZaloPay callback test endpoint hit!");
//        return ResponseEntity.ok("ZaloPay callback endpoint is accessible. POST to this URL for actual callback.");
//    }
//}
