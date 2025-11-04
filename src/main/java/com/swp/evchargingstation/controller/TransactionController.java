package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StaffTransactionResponse;
import com.swp.evchargingstation.service.StaffDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-stations/pending-payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Pending Payments", description = "Danh sách thanh toán chưa xử lý tại trạm của nhân viên")
public class TransactionController {

    StaffDashboardService staffDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "Lấy danh sách phiên sạc chưa thanh toán",
            description = "Staff lấy danh sách tất cả các phiên sạc tại trạm của mình cần xử lý thanh toán"
    )
    public ApiResponse<List<StaffTransactionResponse>> getPendingPayments() {
        log.info("Staff requesting pending payments at their station");
        return ApiResponse.<List<StaffTransactionResponse>>builder()
                .result(staffDashboardService.getStaffTransactions())
                .build();
    }
}
