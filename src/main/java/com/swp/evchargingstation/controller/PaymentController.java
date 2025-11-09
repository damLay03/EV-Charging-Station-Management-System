package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.CashPaymentRequest;
import com.swp.evchargingstation.dto.request.PaymentMethodCreationRequest;
import com.swp.evchargingstation.dto.request.StaffPaymentRequest;
import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.service.CashPaymentService;
import com.swp.evchargingstation.service.PaymentMethodService;
import com.swp.evchargingstation.service.StaffDashboardService;
import com.swp.evchargingstation.service.StationService;
import com.swp.evchargingstation.service.ZaloPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Payments Management", description = "RESTful API quản lý thanh toán (Cash, ZaloPay, Transactions, Payment Methods, Payment History) - Phân quyền theo role")
public class PaymentController {

    StaffDashboardService staffDashboardService;
    CashPaymentService cashPaymentService;
    ZaloPayService zaloPayService;
    PaymentMethodService paymentMethodService;
    StationService stationService;

    // ==================== DRIVER - CASH PAYMENT REQUEST ====================

    @PostMapping("/cash/request")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Yêu cầu thanh toán bằng tiền mặt",
            description = "Driver gửi yêu cầu thanh toán bằng tiền mặt cho session đã hoàn thành. Yêu cầu sẽ được gửi đến staff quản lý trạm"
    )
    public ApiResponse<CashPaymentRequestResponse> requestCashPayment(
            @RequestBody @Valid CashPaymentRequest request) {
        log.info("Driver requesting cash payment for session: {}", request.getSessionId());
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.requestCashPayment(request.getSessionId()))
                .build();
    }

    // ==================== DRIVER - ZALOPAY ====================

    @PostMapping("/zalopay")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Tạo thanh toán ZaloPay",
            description = "Tạo một đơn thanh toán ZaloPay cho phiên sạc. Trả về URL thanh toán để driver chuyển hướng sang cổng thanh toán ZaloPay"
    )
    public ApiResponse<String> createZaloPayPayment(@RequestParam String sessionId) {
        log.info("Driver creating ZaloPay payment for session: {}", sessionId);
        String paymentUrl = zaloPayService.createPayment(sessionId);
        return ApiResponse.<String>builder()
                .result(paymentUrl)
                .build();
    }

    // ==================== STAFF - PAYMENT PROCESSING ====================

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Xử lý thanh toán cho driver",
            description = "Staff xử lý thanh toán bằng tiền mặt hoặc thẻ cho các phiên sạc đã hoàn thành"
    )
    public ApiResponse<String> processPayment(@RequestBody @Valid StaffPaymentRequest request) {
        log.info("Staff processing payment for session: {}", request.getSessionId());
        return staffDashboardService.processPaymentForDriver(request);
    }

    @GetMapping("/cash")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Lấy danh sách yêu cầu thanh toán tiền mặt",
            description = "Lấy danh sách các yêu cầu thanh toán bằng tiền mặt theo trạng thái. " +
                    "Status: PENDING (đang chờ xác nhận), COMPLETED (đã xác nhận)"
    )
    public ApiResponse<List<CashPaymentRequestResponse>> getCashPayments(
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

    @PatchMapping("/cash/{paymentId}/confirm")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Xác nhận đã nhận tiền mặt từ driver",
            description = "Staff xác nhận driver đã thanh toán tiền mặt. Sau khi xác nhận, trạng thái thanh toán sẽ chuyển thành COMPLETED"
    )
    public ApiResponse<CashPaymentRequestResponse> confirmCashPayment(
            @PathVariable String paymentId) {
        log.info("Staff confirming cash payment: {}", paymentId);
        return ApiResponse.<CashPaymentRequestResponse>builder()
                .result(cashPaymentService.confirmCashPayment(paymentId))
                .build();
    }

    // ==================== STAFF - SESSIONS ====================

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Lấy danh sách phiên sạc theo trạng thái thanh toán",
            description = "Staff lấy danh sách các phiên sạc tại trạm của mình theo trạng thái. " +
                    "Status: UNPAID (chưa thanh toán), PAID (đã thanh toán)"
    )
    public ApiResponse<List<StaffTransactionResponse>> getSessions(
            @Parameter(description = "Trạng thái thanh toán", example = "UNPAID")
            @RequestParam(defaultValue = "UNPAID") String status) {
        log.info("Staff requesting sessions at their station with status: {}", status);

        if ("UNPAID".equalsIgnoreCase(status)) {
            return ApiResponse.<List<StaffTransactionResponse>>builder()
                    .result(staffDashboardService.getStaffTransactions())
                    .build();
        }

        throw new IllegalArgumentException("Invalid status: " + status + ". Supported: UNPAID");
    }

    // ==================== DRIVER - PAYMENT METHODS ====================

    @PostMapping("/methods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Thêm phương thức thanh toán mới",
            description = "Driver thêm một phương thức thanh toán mới (thẻ tín dụng, ví điện tử, ...) vào tài khoản của mình"
    )
    public ApiResponse<PaymentMethodResponse> addPaymentMethod(
            Authentication authentication,
            @RequestBody @Valid PaymentMethodCreationRequest request) {
        String driverId = authentication.getName();
        log.info("Driver {} adding payment method", driverId);

        return ApiResponse.<PaymentMethodResponse>builder()
                .result(paymentMethodService.create(driverId, request))
                .build();
    }

    @GetMapping("/methods")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy danh sách phương thức thanh toán của tôi",
            description = "Trả về danh sách tất cả các phương thức thanh toán mà driver đã lưu, bao gồm thẻ tín dụng, ví điện tử và các phương thức khác"
    )
    public ApiResponse<List<PaymentMethodResponse>> getMyPaymentMethods(Authentication authentication) {
        String driverId = authentication.getName();
        log.info("Driver {} fetching payment methods", driverId);

        return ApiResponse.<List<PaymentMethodResponse>>builder()
                .result(paymentMethodService.getMyPaymentMethods(driverId))
                .build();
    }

    @DeleteMapping("/methods/{pmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Xóa phương thức thanh toán",
            description = "Xóa một phương thức thanh toán khỏi tài khoản của driver. Driver chỉ có thể xóa các phương thức thanh toán của chính mình"
    )
    public ApiResponse<Void> deletePaymentMethod(
            Authentication authentication,
            @PathVariable String pmId) {
        String driverId = authentication.getName();
        log.info("Driver {} deleting payment method {}", driverId, pmId);

        paymentMethodService.delete(driverId, pmId);
        return ApiResponse.<Void>builder()
                .message("Payment method deleted successfully")
                .build();
    }



    // ==================== ADMIN/STAFF - PAYMENT HISTORY ====================

    @GetMapping("/history/stations/{stationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "[ADMIN/STAFF] Lấy lịch sử thanh toán của trạm sạc",
            description = "Trả về lịch sử thanh toán của một trạm sạc. ADMIN có thể xem lịch sử của bất kỳ trạm nào, STAFF chỉ xem trạm mình quản lý. Có thể lọc theo ngày và phương thức thanh toán"
    )
    public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistory(
            @Parameter(description = "ID của trạm sạc", example = "STATION_123")
            @PathVariable String stationId,
            @Parameter(description = "Ngày bắt đầu (định dạng: yyyy-MM-dd)", example = "2025-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (định dạng: yyyy-MM-dd)", example = "2025-11-04")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Phương thức thanh toán", example = "CASH")
            @RequestParam(required = false) Payment.PaymentMethod paymentMethod) {

        log.info("Fetching payment history for station {} - startDate: {}, endDate: {}, paymentMethod: {}",
                stationId, startDate, endDate, paymentMethod);

        return ApiResponse.<List<PaymentHistoryResponse>>builder()
                .result(stationService.getPaymentHistory(stationId, startDate, endDate, paymentMethod))
                .build();
    }
}

