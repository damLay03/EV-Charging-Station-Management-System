package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.request.PlanUpdateRequest;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Plan;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.BillingType;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.PlanMapper;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.PlanRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlanService {
    PlanRepository planRepository;
    PlanMapper planMapper;
    DriverRepository driverRepository;
    WalletService walletService;
    EmailService emailService;

    // NOTE: Tạo plan theo billingType truyền trong request (dùng cho endpoint generic). Áp dụng rule validate theo loại.
    @Transactional
    public PlanResponse create(PlanCreationRequest request) {
        BillingType billingType = request.getBillingType();
        log.info("Creating plan name='{}' type={} ", request.getName(), billingType);
        validateNameUnique(request.getName());
        validateConfig(billingType, request);
        Plan plan = Plan.builder()
                .name(request.getName())
                .billingType(billingType)
                .pricePerKwh(request.getPricePerKwh())
                .pricePerMinute(request.getPricePerMinute())
                .monthlyFee(request.getMonthlyFee())
                .benefits(request.getBenefits())
                .build();
        return planMapper.toPlanResponse(planRepository.save(plan));
    }

//    // NOTE: Tạo gói PREPAID (override billingType bất kể body gửi gì)
//    @Transactional
//    public PlanResponse createPrepaid(PlanCreationRequest request) {
//        request.setBillingType(BillingType.PREPAID);
//        return create(request);
//    }

//    // NOTE: Tạo gói POSTPAID (override billingType)
//    @Transactional
//    public PlanResponse createPostpaid(PlanCreationRequest request) {
//        request.setBillingType(BillingType.POSTPAID);
//        return create(request);
//    }

    // NOTE: Tạo gói VIP có monthlyFee > 0 (override billingType)
    @Transactional
    public PlanResponse createVip(PlanCreationRequest request) {
        request.setBillingType(BillingType.VIP);
        return create(request);
    }

    // NOTE: Lấy tất cả plan hiện có (chưa phân trang)
    @Transactional(readOnly = true)
    public List<PlanResponse> getAll() {
        return planRepository.findAll().stream().map(planMapper::toPlanResponse).toList();
    }

    // NOTE: Lấy chi tiết một plan theo id. Throw PLAN_NOT_FOUND nếu không tồn tại.
    @Transactional(readOnly = true)
    public PlanResponse getById(String planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        return planMapper.toPlanResponse(plan);
    }

    // NOTE: Cập nhật plan theo id. Validate name unique (trừ chính nó), validate config theo billingType mới.
    @Transactional
    public PlanResponse update(String planId, PlanUpdateRequest request) {
        log.info("Updating plan id='{}' with name='{}' type={}", planId, request.getName(), request.getBillingType());
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // Validate name unique (ignore current plan)
        validateNameUniqueForUpdate(request.getName(), planId);

        // Validate config for new billingType
        BillingType billingType = request.getBillingType();
        validateConfigForUpdate(billingType, request);

        // Update fields
        plan.setName(request.getName());
        plan.setBillingType(billingType);
        plan.setPricePerKwh(request.getPricePerKwh());
        plan.setPricePerMinute(request.getPricePerMinute());
        plan.setMonthlyFee(request.getMonthlyFee());
        plan.setBenefits(request.getBenefits());

        return planMapper.toPlanResponse(planRepository.save(plan));
    }

    // NOTE: Xóa plan theo id. Throw PLAN_NOT_FOUND nếu không tồn tại. Throw PLAN_IN_USE nếu đang được sử dụng.
    @Transactional
    public void delete(String planId) {
        log.info("Deleting plan id='{}'", planId);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        planRepository.delete(plan);
        log.info("Plan id='{}' deleted successfully", planId);
    }

    // NOTE: Driver đăng ký/thay đổi plan - Kiểm tra số dư ví, trừ tiền, gửi email
    @Transactional
    public PlanResponse subscribePlan(String driverId, String planId) {
        log.info("Driver {} subscribing to plan {}", driverId, planId);

        // Kiểm tra driver có tồn tại không
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        // Kiểm tra plan có tồn tại không
        Plan newPlan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // Kiểm tra plan hiện tại của driver
        Plan currentPlan = driver.getPlan();
        LocalDateTime subscriptionDate = driver.getPlanSubscriptionDate();

        // Kiểm tra xem gói hiện tại còn hiệu lực không (trong vòng 1 tháng)
        boolean isPlanActive = currentPlan != null &&
                              subscriptionDate != null &&
                              subscriptionDate.plusMonths(1).isAfter(LocalDateTime.now());

        // CHẶN DOWNGRADE: Nếu gói hiện tại còn hiệu lực
        if (isPlanActive && currentPlan != null) {
            double currentPlanFee = (double) currentPlan.getMonthlyFee();
            double newPlanFee = (double) newPlan.getMonthlyFee();

            // Nếu đang cố chuyển sang gói rẻ hơn → CHẶN
            if (newPlanFee < currentPlanFee) {
                log.warn("Driver {} cannot downgrade from {} ({}đ) to {} ({}đ) during active period. Plan active until {}",
                        driverId, currentPlan.getName(), currentPlanFee, newPlan.getName(), newPlanFee,
                        subscriptionDate.plusMonths(1));
                throw new AppException(ErrorCode.CANNOT_DOWNGRADE_TO_FLEXIBLE);
            }

            // Đặc biệt: CHẶN chuyển từ VIP về bất kỳ gói nào (kể cả cùng giá)
            if (currentPlan.getBillingType() == BillingType.VIP &&
                newPlan.getBillingType() != BillingType.VIP) {
                log.warn("Driver {} cannot downgrade from VIP plan {} to {}. VIP is the highest tier.",
                        driverId, currentPlan.getName(), newPlan.getName());
                throw new AppException(ErrorCode.CANNOT_DOWNGRADE_TO_FLEXIBLE);
            }
        }

        // Tính số tiền cần trả dựa trên tình huống
        double amountToCharge = 0.0;
        String transactionDescription = "";

        if (!isPlanActive) {
            // Trường hợp 1: Không có gói hoặc gói đã hết hạn → Thu FULL phí
            amountToCharge = (double) newPlan.getMonthlyFee();
            transactionDescription = "Đăng ký gói " + newPlan.getName();
            log.info("Driver {} subscribing to new plan (no active plan). Full fee: {}", driverId, amountToCharge);
        } else {
            // Trường hợp 2: Đang có gói còn hiệu lực → Xử lý nâng/hạ cấp
            double currentPlanFee = (double) currentPlan.getMonthlyFee();
            double newPlanFee = (double) newPlan.getMonthlyFee();

            if (newPlanFee > currentPlanFee) {
                // NÂNG CẤP: Thu phần chênh lệch
                amountToCharge = newPlanFee - currentPlanFee;
                transactionDescription = String.format("Nâng cấp từ gói %s lên %s (chênh lệch)",
                        currentPlan.getName(), newPlan.getName());
                log.info("Driver {} upgrading from {} to {}. Charging difference: {} VND",
                        driverId, currentPlan.getName(), newPlan.getName(), amountToCharge);
            } else if (newPlanFee < currentPlanFee) {
                // HẠ CẤP: KHÔNG thu tiền (đã trả cho gói cao hơn)
                amountToCharge = 0.0;
                transactionDescription = String.format("Chuyển từ gói %s về %s (không thu thêm phí)",
                        currentPlan.getName(), newPlan.getName());
                log.info("Driver {} downgrading from {} to {}. No additional charge (already paid for higher plan)",
                        driverId, currentPlan.getName(), newPlan.getName());
            } else {
                // Cùng giá → Không thu tiền
                amountToCharge = 0.0;
                transactionDescription = String.format("Chuyển đổi gói %s sang %s (cùng mức phí)",
                        currentPlan.getName(), newPlan.getName());
                log.info("Driver {} switching to same-price plan. No charge", driverId);
            }

            // Giữ nguyên ngày đăng ký cũ để không reset chu kỳ billing
            subscriptionDate = driver.getPlanSubscriptionDate();
        }

        // Kiểm tra số dư và trừ tiền nếu cần
        if (amountToCharge > 0) {
            double currentBalance = walletService.getBalance(driverId);
            if (currentBalance < amountToCharge) {
                log.warn("Driver {} has insufficient funds. Balance: {}, Required: {}",
                        driverId, currentBalance, amountToCharge);
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }

            // Trừ tiền từ ví
            try {
                walletService.debit(
                    driverId,
                    amountToCharge,
                    TransactionType.PLAN_SUBCRIPTION,
                    transactionDescription,
                    null,
                    null
                );
                log.info("Debited {} from driver {} for plan subscription", amountToCharge, driverId);
            } catch (Exception e) {
                log.error("Failed to debit wallet for driver {}: {}", driverId, e.getMessage());
                throw new AppException(ErrorCode.TRANSACTION_FAILED);
            }
        }

        // Cập nhật plan cho driver
        driver.setPlan(newPlan);

        // Chỉ cập nhật ngày đăng ký nếu không có gói cũ hoặc gói cũ đã hết hạn
        if (!isPlanActive) {
            driver.setPlanSubscriptionDate(LocalDateTime.now());
        }
        // Nếu có gói cũ còn hiệu lực, giữ nguyên subscriptionDate để chu kỳ billing không bị reset

        // Luôn bật auto-renewal khi đăng ký plan mới (áp dụng cho tất cả loại gói)
        driver.setPlanAutoRenew(true);

        Driver savedDriver = driverRepository.save(driver);

        log.info("Driver {} successfully subscribed to plan {} at {}. Amount charged: {}",
                driverId, newPlan.getName(), savedDriver.getPlanSubscriptionDate(), amountToCharge);

        // Force load User để tránh LazyInitializationException trong async email
        User user = savedDriver.getUser();
        if (user != null) {
            // Trigger lazy loading
            user.getEmail();
        }

        // Gửi email thông báo đăng ký thành công (không block transaction nếu email fail)
        try {
            emailService.sendPlanSubscriptionSuccessEmail(savedDriver, newPlan, amountToCharge);
        } catch (Exception e) {
            log.error("Failed to send subscription email to driver {}: {}", driverId, e.getMessage());
            // Không throw exception, chỉ log warning
        }

        return enrichPlanResponseWithExpiryInfo(planMapper.toPlanResponse(newPlan), savedDriver);
    }

    // NOTE: Auto renew plan cho driver (chạy scheduled job hàng ngày)
    @Transactional
    public boolean autoRenewPlan(Driver driver) {
        Plan currentPlan = driver.getPlan();

        // Chỉ auto renew cho plan "Cao cấp" và "Tiết kiệm"
        if (currentPlan == null ||
            (!currentPlan.getName().equalsIgnoreCase("Cao cấp") &&
             !currentPlan.getName().equalsIgnoreCase("Tiết kiệm"))) {
            return false;
        }

        double monthlyFee = currentPlan.getMonthlyFee();
        if (monthlyFee <= 0) {
            return false; // Không cần renew nếu không có phí
        }

        // Kiểm tra nếu driver đã hủy gia hạn tự động
        if (driver.getPlanAutoRenew() != null && !driver.getPlanAutoRenew()) {
            log.info("Driver {} has disabled auto-renewal. Downgrading to Flexible plan", driver.getUserId());

            // Chuyển về plan "Linh hoạt"
            Plan flexiblePlan = planRepository.findByNameIgnoreCase("Linh hoạt")
                    .orElse(null);

            if (flexiblePlan != null) {
                driver.setPlan(flexiblePlan);
                driver.setPlanSubscriptionDate(LocalDateTime.now());
                driver.setPlanAutoRenew(true); // Reset về true cho lần sau
                driverRepository.save(driver);

                log.info("Downgraded driver {} to flexible plan due to canceled auto-renewal", driver.getUserId());

                // Gửi email thông báo chuyển về Linh hoạt
                try {
                    emailService.sendPlanDowngradedToFlexibleEmail(driver, currentPlan, flexiblePlan);
                } catch (Exception e) {
                    log.error("Failed to send downgrade email: {}", e.getMessage());
                }
            }

            return false;
        }

        try {
            // Kiểm tra số dư ví
            double currentBalance = walletService.getBalance(driver.getUserId());
            if (currentBalance < monthlyFee) {
                log.warn("Driver {} has insufficient funds for auto renew. Balance: {}, Required: {}",
                        driver.getUserId(), currentBalance, monthlyFee);

                // Chuyển về plan "Linh hoạt"
                Plan flexiblePlan = planRepository.findByNameIgnoreCase("Linh hoạt")
                        .orElse(null);

                if (flexiblePlan != null) {
                    driver.setPlan(flexiblePlan);
                    driver.setPlanSubscriptionDate(LocalDateTime.now());
                    driver.setPlanAutoRenew(true); // Reset về true
                    driverRepository.save(driver);

                    log.info("Downgraded driver {} to flexible plan due to insufficient funds",
                            driver.getUserId());

                    // Gửi email thông báo renew thất bại
                    emailService.sendPlanRenewalFailedEmail(driver, currentPlan, flexiblePlan, monthlyFee);
                }

                return false;
            }

            // Trừ tiền từ ví
            walletService.debit(
                driver.getUserId(),
                monthlyFee,
                TransactionType.PLAN_SUBCRIPTION,
                "Gia hạn gói " + currentPlan.getName(),
                null,
                null
            );

            // Cập nhật ngày gia hạn
            driver.setPlanSubscriptionDate(LocalDateTime.now());
            driverRepository.save(driver);

            log.info("Auto renewed plan {} for driver {}. Debited: {}",
                    currentPlan.getName(), driver.getUserId(), monthlyFee);

            // Gửi email thông báo gia hạn thành công
            emailService.sendPlanRenewalSuccessEmail(driver, currentPlan, monthlyFee);

            return true;

        } catch (Exception e) {
            log.error("Failed to auto renew plan for driver {}: {}",
                    driver.getUserId(), e.getMessage(), e);
            return false;
        }
    }

    // NOTE: Kiểm tra name trùng (case-insensitive). Throw PLAN_NAME_EXISTED nếu đã tồn tại.
    private void validateNameUnique(String name) {
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.PLAN_NAME_EXISTED);
        }
    }

    // NOTE: Kiểm tra name trùng khi update (bỏ qua plan hiện tại). Throw PLAN_NAME_EXISTED nếu có plan khác dùng name này.
    private void validateNameUniqueForUpdate(String name, String currentPlanId) {
        planRepository.findByNameIgnoreCase(name).ifPresent(existingPlan -> {
            if (!existingPlan.getPlanId().equals(currentPlanId)) {
                throw new AppException(ErrorCode.PLAN_NAME_EXISTED);
            }
        });
    }

    // NOTE: Rule nghiệp vụ cho từng BillingType (điều kiện monthlyFee / price). Throw INVALID_PLAN_CONFIG nếu sai.
    private void validateConfig(BillingType billingType, PlanCreationRequest r) {
        // Simple business validation rules (adjust later as needed)
        switch (billingType) {
//            case PREPAID -> {
//                // Prepaid: no monthly fee, must have usage price
//                if (r.getMonthlyFee() > 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//            }
//            case POSTPAID -> {
//                // Postpaid: user is billed later, similar to pay as you go: monthlyFee must be 0
//                if (r.getMonthlyFee() > 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//            }
            case VIP -> {
                // VIP: must have a monthly fee > 0, usage price can be 0 or discounted
                if (r.getMonthlyFee() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
            case PAY_AS_YOU_GO, MONTHLY_SUBSCRIPTION -> {
                // For completeness if later reused via generic endpoint
                // MONTHLY_SUBSCRIPTION should have monthlyFee > 0
                if (billingType == BillingType.MONTHLY_SUBSCRIPTION && r.getMonthlyFee() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
                if (billingType == BillingType.PAY_AS_YOU_GO && r.getMonthlyFee() > 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
        }
    }

    // NOTE: Validate config cho update request (tương tự validateConfig nhưng dùng PlanUpdateRequest)
    private void validateConfigForUpdate(BillingType billingType, PlanUpdateRequest r) {
        switch (billingType) {
//            case PREPAID -> {
//                if (r.getMonthlyFee() > 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//            }
//            case POSTPAID -> {
//                if (r.getMonthlyFee() > 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
//                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
//                }
//            }
            case VIP -> {
                if (r.getMonthlyFee() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
            case PAY_AS_YOU_GO, MONTHLY_SUBSCRIPTION -> {
                if (billingType == BillingType.MONTHLY_SUBSCRIPTION && r.getMonthlyFee() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
                if (billingType == BillingType.PAY_AS_YOU_GO && r.getMonthlyFee() > 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
        }
    }

    // NOTE: Hủy gia hạn tự động - Driver vẫn dùng gói hiện tại đến hết tháng, sau đó chuyển về "Linh hoạt"
    @Transactional
    public PlanResponse cancelAutoRenewal(String driverId) {
        log.info("Driver {} canceling auto-renewal", driverId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        Plan currentPlan = driver.getPlan();

        // Kiểm tra có gói hiện tại không
        if (currentPlan == null) {
            throw new AppException(ErrorCode.PLAN_NOT_FOUND);
        }

        // Chỉ cho phép hủy gia hạn với gói MONTHLY hoặc VIP
        if (currentPlan.getBillingType() != BillingType.MONTHLY_SUBSCRIPTION &&
            currentPlan.getBillingType() != BillingType.VIP) {
            log.warn("Driver {} trying to cancel auto-renewal for non-subscription plan", driverId);
            throw new AppException(ErrorCode.PLAN_NOT_FOUND);
        }

        // Đánh dấu hủy gia hạn tự động
        driver.setPlanAutoRenew(false);
        driverRepository.save(driver);

        log.info("Driver {} successfully canceled auto-renewal for plan {}. Plan will remain active until {}",
                driverId, currentPlan.getName(),
                driver.getPlanSubscriptionDate() != null ? driver.getPlanSubscriptionDate().plusMonths(1) : "N/A");

        // Gửi email thông báo hủy gia hạn
        try {
            User user = driver.getUser();
            if (user != null) {
                user.getEmail(); // Force load
            }
            emailService.sendPlanCancellationEmail(driver, currentPlan);
        } catch (Exception e) {
            log.error("Failed to send cancellation email to driver {}: {}", driverId, e.getMessage());
        }

        return enrichPlanResponseWithExpiryInfo(planMapper.toPlanResponse(currentPlan), driver);
    }

    // NOTE: Kích hoạt lại gia hạn tự động
    @Transactional
    public PlanResponse reactivateAutoRenewal(String driverId) {
        log.info("Driver {} reactivating auto-renewal", driverId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        Plan currentPlan = driver.getPlan();

        // Kiểm tra có gói hiện tại không
        if (currentPlan == null) {
            throw new AppException(ErrorCode.PLAN_NOT_FOUND);
        }

        // Chỉ cho phép kích hoạt lại với gói MONTHLY hoặc VIP
        if (currentPlan.getBillingType() != BillingType.MONTHLY_SUBSCRIPTION &&
            currentPlan.getBillingType() != BillingType.VIP) {
            log.warn("Driver {} trying to reactivate auto-renewal for non-subscription plan", driverId);
            throw new AppException(ErrorCode.PLAN_NOT_FOUND);
        }

        // Kích hoạt lại gia hạn tự động
        driver.setPlanAutoRenew(true);
        driverRepository.save(driver);

        log.info("Driver {} successfully reactivated auto-renewal for plan {}", driverId, currentPlan.getName());

        // Gửi email thông báo kích hoạt lại
        try {
            User user = driver.getUser();
            if (user != null) {
                user.getEmail(); // Force load
            }
            emailService.sendPlanReactivationEmail(driver, currentPlan);
        } catch (Exception e) {
            log.error("Failed to send reactivation email to driver {}: {}", driverId, e.getMessage());
        }

        return enrichPlanResponseWithExpiryInfo(planMapper.toPlanResponse(currentPlan), driver);
    }

    // ========== HELPER METHOD ==========

    /**
     * Enrich PlanResponse với thông tin expiry date từ Driver
     */
    private PlanResponse enrichPlanResponseWithExpiryInfo(PlanResponse response, Driver driver) {
        if (driver.getPlanSubscriptionDate() != null) {
            LocalDateTime subscriptionDate = driver.getPlanSubscriptionDate();
            LocalDateTime expiryDate = subscriptionDate.plusMonths(1); // Plan có hiệu lực 1 tháng

            response.setPlanSubscriptionDate(subscriptionDate);
            response.setPlanExpiryDate(expiryDate);

            // Tính số ngày còn lại
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(),
                expiryDate
            );
            response.setDaysUntilExpiry(daysUntilExpiry);
        }

        response.setAutoRenewEnabled(driver.getPlanAutoRenew());
        return response;
    }
}
