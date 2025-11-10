package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.request.PlanUpdateRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.service.DashboardService;
import com.swp.evchargingstation.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Plans Management", description = "RESTful API quản lý gói cước - Phân quyền theo role")
public class PlanController {

    PlanService planService;
    DashboardService dashboardService;

    // ==================== DRIVER ENDPOINTS ====================

    @GetMapping("/my-plan")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy gói plan hiện tại của driver",
            description = "Trả về thông tin chi tiết về gói plan mà driver hiện đang sử dụng, bao gồm giá, lợi ích, thời hạn"
    )
    public ApiResponse<PlanResponse> getCurrentPlan() {
        log.info("Driver requesting current plan information");
        return ApiResponse.<PlanResponse>builder()
                .result(dashboardService.getCurrentPlan())
                .build();
    }

    @PostMapping("/subscribe/{planId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Đăng ký/thay đổi gói plan",
            description = "Driver chọn một gói plan mới từ danh sách. Hệ thống sẽ kiểm tra số dư ví, trừ tiền nếu đủ, và gửi email thông báo. Plan cũ (nếu có) sẽ được thay thế bằng plan mới."
    )
    public ApiResponse<PlanResponse> subscribePlan(
            @Parameter(description = "ID của gói plan muốn đăng ký", example = "PLAN_123")
            @PathVariable String planId) {
        log.info("Driver subscribing to plan: {}", planId);

        // Lấy userId từ JWT token
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            log.error("Could not extract userId from JWT token");
            throw new RuntimeException("User ID not found in token");
        }

        return ApiResponse.<PlanResponse>builder()
                .result(planService.subscribePlan(userId, planId))
                .message("Successfully subscribed to plan")
                .build();
    }

    // ==================== ADMIN ENDPOINTS ====================

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Tạo gói cước mới",
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
            summary = "[ADMIN] Lấy chi tiết gói cước",
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
            summary = "[ADMIN] Cập nhật gói cước",
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
    @ResponseStatus(HttpStatus.NO_CONTENT)

    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa gói cước",
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

