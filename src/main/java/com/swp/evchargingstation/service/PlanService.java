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
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // Kiểm tra số dư ví có đủ để trả monthly fee không
        double monthlyFee = (double) plan.getMonthlyFee();
        if (monthlyFee > 0) {
            double currentBalance = walletService.getBalance(driverId);
            if (currentBalance < monthlyFee) {
                log.warn("Driver {} has insufficient funds. Balance: {}, Required: {}",
                        driverId, currentBalance, monthlyFee);
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }

            // Trừ tiền từ ví
            try {
                walletService.debit(
                    driverId,
                    monthlyFee,
                    TransactionType.PLAN_SUBCRIPTION,
                    "Đăng ký gói " + plan.getName(),
                    null,
                    null
                );
                log.info("Debited {} from driver {} for plan subscription", monthlyFee, driverId);
            } catch (Exception e) {
                log.error("Failed to debit wallet for driver {}: {}", driverId, e.getMessage());
                throw new AppException(ErrorCode.TRANSACTION_FAILED);
            }
        }

        // Cập nhật plan và ngày đăng ký cho driver
        driver.setPlan(plan);
        driver.setPlanSubscriptionDate(LocalDateTime.now());
        Driver savedDriver = driverRepository.save(driver);

        log.info("Driver {} successfully subscribed to plan {} at {}",
                driverId, plan.getName(), savedDriver.getPlanSubscriptionDate());

        // Force load User để tránh LazyInitializationException trong async email
        User user = savedDriver.getUser();
        if (user != null) {
            // Trigger lazy loading
            user.getEmail();
        }

        // Gửi email thông báo đăng ký thành công (không block transaction nếu email fail)
        try {
            emailService.sendPlanSubscriptionSuccessEmail(savedDriver, plan, monthlyFee);
        } catch (Exception e) {
            log.error("Failed to send subscription email to driver {}: {}", driverId, e.getMessage());
            // Không throw exception, chỉ log warning
        }

        return planMapper.toPlanResponse(plan);
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
}
