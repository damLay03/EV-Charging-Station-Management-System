package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.request.SubscriptionCreationRequest;
import com.swp.evchargingstation.dto.response.SubscriptionResponse;
import com.swp.evchargingstation.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionController {
    SubscriptionService subscriptionService;

    // NOTE: Driver đăng ký gói subscription (Free, Premium, VIP)
    // Nếu gói có phí monthly, cần cung cấp paymentMethodId
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<SubscriptionResponse> subscribe(
            Authentication authentication,
            @RequestBody @Valid SubscriptionCreationRequest request) {
        String driverId = authentication.getName();
        log.info("Driver {} subscribing to plan", driverId);

        return ApiResponse.<SubscriptionResponse>builder()
                .result(subscriptionService.subscribe(driverId, request))
                .build();
    }

    // NOTE: Driver xem subscription hiện tại (ACTIVE)
    @GetMapping("/active")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<SubscriptionResponse> getMyActiveSubscription(Authentication authentication) {
        String driverId = authentication.getName();
        log.info("Driver {} fetching active subscription", driverId);

        return ApiResponse.<SubscriptionResponse>builder()
                .result(subscriptionService.getMyActiveSubscription(driverId))
                .build();
    }

    // NOTE: Driver hủy subscription hiện tại
    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<Void> cancelSubscription(
            Authentication authentication,
            @PathVariable String subscriptionId) {
        String driverId = authentication.getName();
        log.info("Driver {} cancelling subscription {}", driverId, subscriptionId);

        subscriptionService.cancelSubscription(driverId, subscriptionId);
        return ApiResponse.<Void>builder()
                .message("Subscription cancelled successfully")
                .build();
    }

    // NOTE: Driver bật/tắt auto-renew
    @PatchMapping("/{subscriptionId}/auto-renew")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<SubscriptionResponse> updateAutoRenew(
            Authentication authentication,
            @PathVariable String subscriptionId,
            @RequestParam boolean autoRenew) {
        String driverId = authentication.getName();
        log.info("Driver {} updating auto-renew to {} for subscription {}",
                driverId, autoRenew, subscriptionId);

        return ApiResponse.<SubscriptionResponse>builder()
                .result(subscriptionService.updateAutoRenew(driverId, subscriptionId, autoRenew))
                .build();
    }
}

