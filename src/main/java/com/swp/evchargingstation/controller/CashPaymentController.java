package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.CashPaymentRequestResponse;
import com.swp.evchargingstation.service.CashPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cash Payment", description = "API quản lý thanh toán bằng tiền mặt")
public class CashPaymentController {

    CashPaymentService cashPaymentService;

    @PostMapping("/{sessionId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver yêu cầu thanh toán bằng tiền mặt",
            description = "Driver gửi yêu cầu thanh toán bằng tiền mặt cho session đã hoàn thành. " +
                    "Yêu cầu sẽ được gửi đến staff quản lý trạm.")
    public ApiResponse<CashPaymentRequestResponse> requestCashPayment(
            @PathVariable String sessionId) {
        log.info("Received cash payment request for session: {}", sessionId);
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.requestCashPayment(sessionId))
                .build();
    }

    @GetMapping("/staff/pending")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff xem danh sách yêu cầu thanh toán tiền mặt đang chờ",
            description = "Lấy danh sách các yêu cầu thanh toán bằng tiền mặt đang chờ xác nhận tại trạm của staff")
    public ApiResponse<List<CashPaymentRequestResponse>> getPendingCashPaymentRequests() {
        log.info("Staff getting pending cash payment requests");
        return ApiResponse.<List<CashPaymentRequestResponse>>builder()
                .result(cashPaymentService.getPendingCashPaymentRequests())
                .build();
    }

    @GetMapping("/staff/history")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff xem lịch sử thanh toán tiền mặt đã xác nhận",
            description = "Lấy danh sách tất cả các thanh toán tiền mặt mà staff đã xác nhận. " +
                    "Danh sách được sắp xếp theo thời gian xác nhận mới nhất.")
    public ApiResponse<List<CashPaymentRequestResponse>> getConfirmedCashPaymentHistory() {
        log.info("Staff getting confirmed cash payment history");
        return ApiResponse.<List<CashPaymentRequestResponse>>builder()
                .result(cashPaymentService.getConfirmedCashPaymentHistory())
                .build();
    }

    @PutMapping("/staff/{paymentId}/confirm")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff xác nhận đã nhận tiền mặt từ driver",
            description = "Staff xác nhận driver đã thanh toán tiền mặt. " +
                    "Sau khi xác nhận, trạng thái thanh toán sẽ chuyển thành COMPLETED.")
    public ApiResponse<CashPaymentRequestResponse> confirmCashPayment(
            @PathVariable String paymentId) {
        log.info("Staff confirming cash payment: {}", paymentId);
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.confirmCashPayment(paymentId))
                .build();
    }
}
