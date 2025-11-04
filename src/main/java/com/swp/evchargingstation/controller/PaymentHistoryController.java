package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.PaymentHistoryResponse;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stations/{stationId}/payment-history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Station Payment History", description = "Lịch sử thanh toán của trạm sạc")
public class PaymentHistoryController {

    StationService stationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Lấy lịch sử thanh toán của trạm sạc",
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
