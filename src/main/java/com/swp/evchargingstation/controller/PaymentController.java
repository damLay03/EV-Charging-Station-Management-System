package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.CashPaymentRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.service.CashPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Payment", description = "Payment APIs - Cash Payment Only")
public class PaymentController {

    CashPaymentService cashPaymentService;

    @PostMapping("/cash/request")
    @Operation(summary = "Request cash payment",
               description = "Driver requests to pay in cash - payment will be sent to station staff for confirmation")
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
}
