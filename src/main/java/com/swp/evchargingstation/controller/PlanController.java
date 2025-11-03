package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.PlanCreationRequest;
import com.swp.evchargingstation.dto.request.PlanUpdateRequest;
import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Plans", description = "API quản lý các gói cước (Plan) của hệ thống")
public class PlanController {
    PlanService planService;

    // NOTE: Endpoint generic tạo plan theo billingType trong body (PAY_AS_YOU_GO / MONTHLY_SUBSCRIPTION / PREPAID / POSTPAID / VIP).
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo gói cước mới",
            description = "Tạo một gói cước mới với các loại thanh toán khác nhau (Trả theo sử dụng, Đăng ký tháng, Trả trước, Trả sau, VIP). Chỉ quản trị viên có quyền tạo"
    )
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
    @Operation(
            summary = "Lấy danh sách tất cả gói cước",
            description = "Trả về danh sách tất cả các gói cước trong hệ thống. Dùng cho quản trị viên quản lý các gói cước"
    )
    public ApiResponse<List<PlanResponse>> getAll() {
        return ApiResponse.<List<PlanResponse>>builder()
                .result(planService.getAll())
                .build();
    }

    // NOTE: Lấy chi tiết 1 plan theo id (dùng để xem thông tin trước khi gán cho user/subscription).
    @GetMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy chi tiết gói cước theo ID",
            description = "Trả về thông tin chi tiết của một gói cước cụ thể theo ID. Dùng để xem thông tin gói trước khi gán cho người dùng hoặc subscription"
    )
    public ApiResponse<PlanResponse> getById(@PathVariable String planId) {
        return ApiResponse.<PlanResponse>builder()
                .result(planService.getById(planId))
                .build();
    }

    // NOTE: Cập nhật plan theo id. Chỉ ADMIN mới có quyền. Validate name unique và config theo billingType.
    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật gói cước",
            description = "Cập nhật thông tin chi tiết của một gói cước theo ID. Chỉ quản trị viên có quyền. Kiểm tra tính duy nhất của tên gói và validate cấu hình theo loại thanh toán"
    )
    public ApiResponse<PlanResponse> update(@PathVariable String planId, @RequestBody @Valid PlanUpdateRequest request) {
        return ApiResponse.<PlanResponse>builder()
                .result(planService.update(planId, request))
                .build();
    }

    // NOTE: Xóa plan theo id. Chỉ ADMIN mới có quyền.
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa gói cước",
            description = "Xóa một gói cước khỏi hệ thống theo ID. Chỉ quản trị viên có quyền xóa. Gói cước đã xóa sẽ không thể được sử dụng nữa"
    )
    public ApiResponse<Void> delete(@PathVariable String planId) {
        planService.delete(planId);
        return ApiResponse.<Void>builder()
                .message("Plan deleted successfully")
                .build();
    }
}
