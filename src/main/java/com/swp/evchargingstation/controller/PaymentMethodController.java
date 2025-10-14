package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.PaymentMethodCreationRequest;
import com.swp.evchargingstation.dto.response.PaymentMethodResponse;
import com.swp.evchargingstation.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentMethodController {
    PaymentMethodService paymentMethodService;

    // NOTE: Driver thêm phương thức thanh toán (Credit Card, E-Wallet, ...)
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<PaymentMethodResponse> create(
            Authentication authentication,
            @RequestBody @Valid PaymentMethodCreationRequest request) {
        String driverId = authentication.getName();
        log.info("Driver {} adding payment method", driverId);

        return ApiResponse.<PaymentMethodResponse>builder()
                .result(paymentMethodService.create(driverId, request))
                .build();
    }

    // NOTE: Driver xem danh sách phương thức thanh toán của mình
    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<PaymentMethodResponse>> getMyPaymentMethods(Authentication authentication) {
        String driverId = authentication.getName();
        log.info("Driver {} fetching payment methods", driverId);

        return ApiResponse.<List<PaymentMethodResponse>>builder()
                .result(paymentMethodService.getMyPaymentMethods(driverId))
                .build();
    }

    // NOTE: Driver xóa phương thức thanh toán
    @DeleteMapping("/{pmId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable String pmId) {
        String driverId = authentication.getName();
        log.info("Driver {} deleting payment method {}", driverId, pmId);

        paymentMethodService.delete(driverId, pmId);
        return ApiResponse.<Void>builder()
                .message("Payment method deleted successfully")
                .build();
    }
}

