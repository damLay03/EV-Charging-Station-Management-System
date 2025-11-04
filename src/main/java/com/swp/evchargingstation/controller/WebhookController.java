package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.service.ZaloPayService;
import com.swp.evchargingstation.dto.zalopay.ZaloPayCallbackRequest;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Webhooks", description = "Webhooks để nhận callback từ payment gateway")
public class WebhookController {

    ZaloPayService zaloPayService;

    @PostMapping("/zalopay/callback")
    @Operation(
            summary = "Xử lý callback từ ZaloPay",
            description = "Endpoint nhận callback từ ZaloPay server để xác nhận kết quả thanh toán. " +
                    "Đây là internal endpoint được gọi bởi ZaloPay, không phải từ client"
    )
    public ResponseEntity<Map<String, Object>> zaloPayCallback(
            @RequestBody ZaloPayCallbackRequest callbackRequest
    ) {
        log.info("=== ZaloPay Webhook Callback Received ===");
        log.info("Data: {}", callbackRequest.getData());
        log.info("MAC: {}", callbackRequest.getMac());

        Map<String, Object> response = zaloPayService.handleCallback(callbackRequest);

        log.info("Webhook response: {}", response);
        log.info("=== End ZaloPay Webhook Callback ===");

        return ResponseEntity.ok(response);
    }
}

