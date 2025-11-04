package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.request.PlanUpdateRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Plans (Admin)", description = "Quản lý gói cước (CRUD) - Admin only")
public class AdminPlanController {

    PlanService planService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo gói cước mới",
            description = "Admin tạo một gói cước mới. Có thể tạo các loại: PREPAID (trả trước), POSTPAID (trả sau), VIP (có phí hàng tháng)"
    )
    public ApiResponse<PlanResponse> createPlan(@RequestBody @Valid PlanCreationRequest request) {
        log.info("Admin creating plan: {}", request.getName());
        return ApiResponse.<PlanResponse>builder()
                .result(planService.create(request))
                .message("Plan created successfully")
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách tất cả gói cước",
            description = "Trả về danh sách tất cả các gói cước có trong hệ thống"
    )
    public ApiResponse<List<PlanResponse>> getAllPlans() {
        log.info("Admin fetching all plans");
        return ApiResponse.<List<PlanResponse>>builder()
                .result(planService.getAll())
                .build();
    }

    @GetMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy chi tiết gói cước",
            description = "Trả về thông tin chi tiết của một gói cước theo ID"
    )
    public ApiResponse<PlanResponse> getPlanById(
            @Parameter(description = "ID của gói cước", example = "PLAN_123")
            @PathVariable String planId) {
        log.info("Admin fetching plan: {}", planId);
        return ApiResponse.<PlanResponse>builder()
                .result(planService.getById(planId))
                .build();
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật gói cước",
            description = "Admin cập nhật thông tin chi tiết của một gói cước"
    )
    public ApiResponse<PlanResponse> updatePlan(
            @Parameter(description = "ID của gói cước", example = "PLAN_123")
            @PathVariable String planId,
            @RequestBody @Valid PlanUpdateRequest request) {
        log.info("Admin updating plan: {}", planId);
        return ApiResponse.<PlanResponse>builder()
                .result(planService.update(planId, request))
                .message("Plan updated successfully")
                .build();
    }

    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa gói cước",
            description = "Admin xóa một gói cước khỏi hệ thống"
    )
    public ApiResponse<Void> deletePlan(
            @Parameter(description = "ID của gói cước", example = "PLAN_123")
            @PathVariable String planId) {
        log.info("Admin deleting plan: {}", planId);
        planService.delete(planId);
        return ApiResponse.<Void>builder()
                .message("Plan deleted successfully")
                .build();
    }
}

