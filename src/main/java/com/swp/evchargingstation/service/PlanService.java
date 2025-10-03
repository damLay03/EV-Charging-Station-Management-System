package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.entity.Plan;
import com.swp.evchargingstation.enums.BillingType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.PlanMapper;
import com.swp.evchargingstation.repository.PlanRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlanService {
    PlanRepository planRepository;
    PlanMapper planMapper;

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

    // NOTE: Tạo gói PREPAID (override billingType bất kể body gửi gì)
    @Transactional
    public PlanResponse createPrepaid(PlanCreationRequest request) {
        request.setBillingType(BillingType.PREPAID);
        return create(request);
    }

    // NOTE: Tạo gói POSTPAID (override billingType)
    @Transactional
    public PlanResponse createPostpaid(PlanCreationRequest request) {
        request.setBillingType(BillingType.POSTPAID);
        return create(request);
    }

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

    // NOTE: Kiểm tra name trùng (case-insensitive). Throw PLAN_NAME_EXISTED nếu đã tồn tại.
    private void validateNameUnique(String name) {
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.PLAN_NAME_EXISTED);
        }
    }

    // NOTE: Rule nghiệp vụ cho từng BillingType (điều kiện monthlyFee / price). Throw INVALID_PLAN_CONFIG nếu sai.
    private void validateConfig(BillingType billingType, PlanCreationRequest r) {
        // Simple business validation rules (adjust later as needed)
        switch (billingType) {
            case PREPAID -> {
                // Prepaid: no monthly fee, must have usage price
                if (r.getMonthlyFee() > 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
            case POSTPAID -> {
                // Postpaid: user is billed later, similar to pay as you go: monthlyFee must be 0
                if (r.getMonthlyFee() > 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
                if (r.getPricePerKwh() <= 0 && r.getPricePerMinute() <= 0) {
                    throw new AppException(ErrorCode.INVALID_PLAN_CONFIG);
                }
            }
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
}
