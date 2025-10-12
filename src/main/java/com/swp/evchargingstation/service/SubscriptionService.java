package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.SubscriptionCreationRequest;
import com.swp.evchargingstation.dto.response.SubscriptionResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.SubscriptionMapper;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionService {
    SubscriptionRepository subscriptionRepository;
    DriverRepository driverRepository;
    PlanRepository planRepository;
    PaymentMethodRepository paymentMethodRepository;
    PaymentRepository paymentRepository;
    SubscriptionMapper subscriptionMapper;

    @Transactional
    public SubscriptionResponse subscribe(String driverId, SubscriptionCreationRequest request) {
        log.info("Driver {} subscribing to plan {}", driverId, request.getPlanId());

        // Kiểm tra driver tồn tại
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        // Kiểm tra plan tồn tại
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // Kiểm tra xem đã có subscription ACTIVE chưa
        Optional<Subscription> existingActiveSubscription = subscriptionRepository
                .findActiveSubscriptionByDriverId(driverId);

        if (existingActiveSubscription.isPresent()) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // Nếu plan có phí monthly, cần payment method
        PaymentMethod paymentMethod = null;
        if (plan.getMonthlyFee() > 0) {
            if (request.getPaymentMethodId() == null || request.getPaymentMethodId().isBlank()) {
                throw new AppException(ErrorCode.PAYMENT_METHOD_REQUIRED);
            }

            paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

            if (!paymentMethod.getDriver().getUserId().equals(driverId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        // Tạo subscription
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(1); // Mặc định 1 tháng

        Subscription subscription = Subscription.builder()
                .driver(driver)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status("ACTIVE")
                .autoRenew(request.isAutoRenew())
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Subscription created with ID: {}", subscription.getSubscriptionId());

        // Tạo payment record nếu có phí
        if (plan.getMonthlyFee() > 0 && paymentMethod != null) {
            Payment payment = Payment.builder()
                    .payer(driver)
                    .amount(plan.getMonthlyFee())
                    .method(paymentMethod)
                    .paymentTime(LocalDateTime.now())
                    .status(PaymentStatus.COMPLETED) // Giả định thanh toán thành công
                    .txnReference("TXN-" + UUID.randomUUID())
                    .subscription(subscription)
                    .build();

            paymentRepository.save(payment);
            log.info("Payment created for subscription");
        }

        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    public SubscriptionResponse getMyActiveSubscription(String driverId) {
        log.info("Getting active subscription for driver: {}", driverId);

        Subscription subscription = subscriptionRepository
                .findActiveSubscriptionByDriverId(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Transactional
    public void cancelSubscription(String driverId, String subscriptionId) {
        log.info("Driver {} cancelling subscription {}", driverId, subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getDriver().getUserId().equals(driverId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        subscription.setStatus("CANCELLED");
        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        log.info("Subscription cancelled successfully");
    }

    @Transactional
    public SubscriptionResponse updateAutoRenew(String driverId, String subscriptionId, boolean autoRenew) {
        log.info("Driver {} updating auto-renew to {} for subscription {}", driverId, autoRenew, subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getDriver().getUserId().equals(driverId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        subscription.setAutoRenew(autoRenew);
        subscription = subscriptionRepository.save(subscription);

        return subscriptionMapper.toSubscriptionResponse(subscription);
    }
}
