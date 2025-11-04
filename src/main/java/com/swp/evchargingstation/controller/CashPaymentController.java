package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.CashPaymentRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.CashPaymentRequestResponse;
import com.swp.evchargingstation.service.CashPaymentService;
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
@RequestMapping("/api/cash-payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Cash Payments", description = "API quản lý thanh toán bằng tiền mặt")
public class CashPaymentController {

    CashPaymentService cashPaymentService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Driver yêu cầu thanh toán bằng tiền mặt",
            description = "Driver gửi yêu cầu thanh toán bằng tiền mặt cho session đã hoàn thành. " +
                    "Yêu cầu sẽ được gửi đến staff quản lý trạm."
    )
    public ApiResponse<CashPaymentRequestResponse> requestCashPayment(
            @RequestBody @Valid CashPaymentRequest request) {
        log.info("Driver requesting cash payment for session: {}", request.getSessionId());
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.requestCashPayment(request.getSessionId()))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Staff lấy danh sách yêu cầu thanh toán tiền mặt",
            description = "Lấy danh sách các yêu cầu thanh toán bằng tiền mặt theo trạng thái. " +
                    "Status: PENDING (đang chờ), COMPLETED (đã xác nhận). " +
                    "Nếu không truyền status, mặc định lấy danh sách PENDING."
    )
    public ApiResponse<List<CashPaymentRequestResponse>> getCashPaymentRequests(
            @Parameter(description = "Trạng thái thanh toán", example = "PENDING")
            @RequestParam(defaultValue = "PENDING") String status) {
        log.info("Staff getting cash payment requests with status: {}", status);

        return switch (status.toUpperCase()) {
            case "COMPLETED" -> ApiResponse.<List<CashPaymentRequestResponse>>builder()
                    .result(cashPaymentService.getConfirmedCashPaymentHistory())
                    .build();
            case "PENDING" -> ApiResponse.<List<CashPaymentRequestResponse>>builder()
                    .result(cashPaymentService.getPendingCashPaymentRequests())
                    .build();
            default -> throw new IllegalArgumentException("Invalid status: " + status);
        };
    }

    @PatchMapping("/{paymentId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Staff xác nhận đã nhận tiền mặt từ driver",
            description = "Staff xác nhận driver đã thanh toán tiền mặt. " +
                    "Sau khi xác nhận, trạng thái thanh toán sẽ chuyển thành COMPLETED."
    )
    public ApiResponse<CashPaymentRequestResponse> confirmCashPayment(
            @PathVariable String paymentId) {
        log.info("Staff confirming cash payment: {}", paymentId);
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.confirmCashPayment(paymentId))
                .build();
    }
}
