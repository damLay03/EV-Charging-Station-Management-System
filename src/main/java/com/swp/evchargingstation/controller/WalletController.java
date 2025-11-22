package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.TopUpCashRequest;
import com.swp.evchargingstation.dto.request.TopUpZaloPayRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.TopUpZaloPayResponse;
import com.swp.evchargingstation.dto.response.WalletBalanceResponse;
import com.swp.evchargingstation.dto.response.WalletDashboardResponse;
import com.swp.evchargingstation.dto.response.WalletTransactionResponse;
import com.swp.evchargingstation.entity.WalletTransaction;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.service.TopUpService;
import com.swp.evchargingstation.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Management", description = "RESTful API quản lý ví điện tử (Số dư, Lịch sử giao dịch, Nạp tiền ZaloPay/Cash, Dashboard) - Phân quyền theo role")
public class WalletController {

    private final WalletService walletService;
    private final TopUpService topUpService;

    /**
     * Extract userId from JWT token
     */
    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("userId");
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * Get current wallet balance
     * Accessible by DRIVER only
     */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy số dư ví hiện tại",
            description = "Lấy thông tin số dư ví điện tử của người dùng đang đăng nhập. " +
                    "API này chỉ dành cho tài xế (DRIVER) để kiểm tra số tiền có sẵn trong ví."
    )
    public ApiResponse<WalletBalanceResponse> getBalance(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Getting wallet balance for user: {}", userId);
        WalletBalanceResponse response = walletService.getWalletBalance(userId);
        return ApiResponse.<WalletBalanceResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Get wallet transaction history with optional filter
     * Accessible by DRIVER only
     * @param type Filter by transaction type: TOPUP (Nạp tiền), CHARGING (Sạc xe), REFUND (Hoàn tiền), or null for all
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy lịch sử giao dịch ví",
            description = "Lấy danh sách các giao dịch trong ví của người dùng với tùy chọn lọc theo loại giao dịch. " +
                    "Có thể lọc theo: TOPUP (Nạp tiền), CHARGING (Thanh toán sạc xe), REFUND (Hoàn tiền), hoặc ALL để xem tất cả."
    )
    public ApiResponse<List<WalletTransactionResponse>> getHistory(
            Authentication authentication,
            @Parameter(description = "Loại giao dịch cần lọc: TOPUP, CHARGING, REFUND, hoặc ALL", example = "ALL")
            @RequestParam(required = false) String type) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Getting wallet history for user: {}, filter: {}", userId, type);

        List<WalletTransactionResponse> history;
        if (type != null && !type.equalsIgnoreCase("ALL")) {
            history = walletService.getTransactionHistoryByType(userId, type);
        } else {
            history = walletService.getTransactionHistory(userId);
        }

        return ApiResponse.<List<WalletTransactionResponse>>builder()
                .result(history)
                .build();
    }

    /**
     * Create ZaloPay top-up order
     * Accessible by DRIVER only
     */
    @PostMapping("/topup/zalopay")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Tạo đơn nạp tiền qua ZaloPay",
            description = "Tạo đơn hàng nạp tiền vào ví điện tử thông qua cổng thanh toán ZaloPay. " +
                    "API sẽ trả về URL thanh toán và mã đơn hàng để người dùng thực hiện thanh toán. " +
                    "Sau khi thanh toán thành công, số tiền sẽ được cộng vào ví tự động."
    )
    public ApiResponse<TopUpZaloPayResponse> createZaloPayTopUp(
            Authentication authentication,
            @Valid @RequestBody TopUpZaloPayRequest request) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Creating ZaloPay top-up for user: {}, amount: {}", userId, request.getAmount());
        TopUpZaloPayResponse response = topUpService.createZaloPayTopUp(userId, request);
        return ApiResponse.<TopUpZaloPayResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Process cash top-up for a user
     * Accessible by STAFF only
     */
    @PostMapping("/topup/cash")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Xử lý nạp tiền mặt cho người dùng",
            description = "Nhân viên (STAFF) sử dụng API này để xử lý việc nạp tiền mặt vào ví cho người dùng. " +
                    "Có thể tìm người dùng bằng email, số điện thoại hoặc userId. " +
                    "Giao dịch sẽ được ghi nhận với thông tin nhân viên xử lý và thời gian thực hiện."
    )
    public ApiResponse<WalletTransactionResponse> processCashTopUp(
            Authentication authentication,
            @Valid @RequestBody TopUpCashRequest request) {
        String staffId = getUserIdFromAuth(authentication);
        log.info("Processing cash top-up by staff: {}, target: {}, amount: {}",
                staffId, request.getTargetUserIdentifier(), request.getAmount());
        WalletTransaction transaction = topUpService.processCashTopUp(staffId, request);

        // Map to response
        WalletTransactionResponse response = WalletTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .description(transaction.getDescription())
                .processedByStaffId(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUserId() : null)
                .processedByStaffName(transaction.getProcessedByStaff() != null ?
                    transaction.getProcessedByStaff().getUser().getFullName() : null)
                .build();

        return ApiResponse.<WalletTransactionResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Get wallet dashboard with statistics
     * Accessible by DRIVER only
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy dashboard và thống kê ví",
            description = "Lấy thông tin tổng quan về ví điện tử bao gồm số dư hiện tại, tổng số tiền đã nạp, " +
                    "tổng chi tiêu cho sạc xe, các giao dịch gần đây và thống kê chi tiết. " +
                    "API này cung cấp cái nhìn tổng quan về tình hình tài chính trong ví của người dùng."
    )
    public ApiResponse<WalletDashboardResponse> getDashboard(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Getting wallet dashboard for user: {}", userId);
        WalletDashboardResponse response = walletService.getWalletDashboard(userId);
        return ApiResponse.<WalletDashboardResponse>builder()
                .result(response)
                .build();
    }
}
