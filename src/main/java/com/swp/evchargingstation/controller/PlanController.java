package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.request.PlanUpdateRequest;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.service.PlanService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanController {
    PlanService planService;

    // NOTE: Endpoint generic tạo plan theo billingType trong body (PAY_AS_YOU_GO / MONTHLY_SUBSCRIPTION / PREPAID / POSTPAID / VIP).
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlanResponse> create(@RequestBody @Valid PlanCreationRequest request) {
        return ApiResponse.<PlanResponse>builder()
                .result(planService.create(request))
                .build();
    }

//    // NOTE: Tạo plan PREPAID (override billingType). Không cần gửi billingType hoặc có cũng bị ghi đè.
//    @PostMapping("/prepaid")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<PlanResponse> createPrepaid(@RequestBody @Valid PlanCreationRequest request) {
//        return ApiResponse.<PlanResponse>builder()
//                .result(planService.createPrepaid(request))
//                .build();
//    }

//    // NOTE: Tạo plan POSTPAID (override billingType).
//    @PostMapping("/postpaid")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<PlanResponse> createPostpaid(@RequestBody @Valid PlanCreationRequest request) {
//        return ApiResponse.<PlanResponse>builder()
//                .result(planService.createPostpaid(request))
//                .build();
//    }

//    // NOTE: Tạo plan VIP (override billingType) yêu cầu monthlyFee > 0.
//    @PostMapping("/vip")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<PlanResponse> createVip(@RequestBody @Valid PlanCreationRequest request) {
//        return ApiResponse.<PlanResponse>builder()
//                .result(planService.createVip(request))
//                .build();
//    }

    // NOTE: Lấy tất cả plan (chưa phân trang, dùng cho admin màn quản trị gói).
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PlanResponse>> getAll() {
        return ApiResponse.<List<PlanResponse>>builder()
                .result(planService.getAll())
                .build();
    }

    // NOTE: Lấy chi tiết 1 plan theo id (dùng để xem thông tin trước khi gán cho user/subscription).
    @GetMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlanResponse> getById(@PathVariable String planId) {
        return ApiResponse.<PlanResponse>builder()
                .result(planService.getById(planId))
                .build();
    }

    // NOTE: Cập nhật plan theo id. Chỉ ADMIN mới có quyền. Validate name unique và config theo billingType.
    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlanResponse> update(@PathVariable String planId, @RequestBody @Valid PlanUpdateRequest request) {
        return ApiResponse.<PlanResponse>builder()
                .result(planService.update(planId, request))
                .build();
    }

    // NOTE: Xóa plan theo id. Chỉ ADMIN mới có quyền.
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String planId) {
        planService.delete(planId);
        return ApiResponse.<Void>builder()
                .message("Plan deleted successfully")
                .build();
    }
}
