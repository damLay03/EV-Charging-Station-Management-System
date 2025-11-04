package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.StaffPaymentRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.PendingPaymentResponse;
import com.swp.evchargingstation.service.StaffDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my-stations/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Staff Station Payments", description = "Xử lý thanh toán tại trạm của nhân viên")
public class PaymentController {

    StaffDashboardService staffDashboardService;

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Xử lý thanh toán cho driver",
            description = "Staff xử lý thanh toán bằng tiền mặt hoặc thẻ cho các phiên sạc đã hoàn thành"
    )
    public ApiResponse<String> processPayment(@RequestBody @Valid StaffPaymentRequest request) {
        log.info("Staff processing payment for session: {}", request.getSessionId());
        return staffDashboardService.processPaymentForDriver(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Lấy danh sách yêu cầu thanh toán đang chờ",
            description = "Lấy danh sách tất cả các yêu cầu thanh toán bằng tiền mặt đang chờ xác nhận tại trạm của staff"
    )
    public ApiResponse<List<PendingPaymentResponse>> getPendingPayments(
            @AuthenticationPrincipal Jwt jwt) {
        String staffId = jwt.getClaim("userId");
        log.info("Staff {} requesting pending payments", staffId);
        return ApiResponse.<List<PendingPaymentResponse>>builder()
                .result(staffDashboardService.getPendingCashPayments(staffId))
                .build();
    }

    @PatchMapping("/{paymentId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Xác nhận thanh toán tiền mặt",
            description = "Staff xác nhận driver đã thanh toán tiền mặt. Trạng thái chuyển thành COMPLETED"
    )
    public ApiResponse<String> confirmPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {
        String staffId = jwt.getClaim("userId");
        log.info("Staff {} confirming payment {}", staffId, paymentId);
        return staffDashboardService.confirmCashPayment(paymentId, staffId);
    }
}
