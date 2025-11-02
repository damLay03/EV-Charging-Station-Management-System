package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.StationCreationRequest;
import com.swp.evchargingstation.dto.request.StationUpdateRequest;
import com.swp.evchargingstation.dto.response.StationDetailResponse;
import com.swp.evchargingstation.dto.response.StationOverviewResponse;
import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.dto.request.ChargingPointCreationRequest;
import com.swp.evchargingstation.dto.request.ChargingPointUpdateRequest;
import com.swp.evchargingstation.dto.response.ChargingPointResponse;
import com.swp.evchargingstation.enums.StationStatus;
import com.swp.evchargingstation.service.StationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationController {
    StationService stationService;

    // Endpoint overview: trả về tất cả trạm + cờ active cho FE (nhẹ hơn so với full StationResponse)
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationOverviewResponse>> getOverview() {
        log.info("Admin fetching station overview");
        return ApiResponse.<List<StationOverviewResponse>>builder()
                .result(stationService.getAllOverview())
                .build();
    }

    // Danh sách trạm với thông tin cơ bản
    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StationResponse>> getStations(
            @RequestParam(value = "status", required = false) StationStatus status) {
        log.info("Admin fetching stations - status: {}", status);
        return ApiResponse.<List<StationResponse>>builder()
                .result(stationService.getStations(status))
                .build();
    }

    // NOTE: Danh sách trạm với thông tin đầy đủ cho UI quản lý (bao gồm điểm sạc, doanh thu, % sử dụng, nhân viên)
    @GetMapping("/list-detail")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ApiResponse<List<StationDetailResponse>> getStationsDetail(
            @RequestParam(value = "status", required = false) StationStatus status) {
        log.info("Admin fetching stations with detail - status: {}", status);
        return ApiResponse.<List<StationDetailResponse>>builder()
                .result(stationService.getStationsWithDetail(status))
                .build();
    }

    // Cập nhật trạng thái cụ thể (truyền enum trực tiếp)
    @PatchMapping("/{stationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> updateStationStatus(@PathVariable String stationId, @RequestParam StationStatus status) {
        log.info("Admin updating station {} to status {}", stationId, status);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStationStatus(stationId, status))
                .build();
    }

    // Đặt trạng thái hoạt động (OPERATIONAL)
    @PatchMapping("/{stationId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> activate(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.activate(stationId))
                .build();
    }

    // Đặt trạng thái OUT_OF_SERVICE (ngưng hoạt động)
    @PatchMapping("/{stationId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> deactivate(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.deactivate(stationId))
                .build();
    }

    // Toggle giữa OPERATIONAL và OUT_OF_SERVICE
    @PatchMapping("/{stationId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> toggle(@PathVariable String stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.toggle(stationId))
                .build();
    }

    // ========== STAFF ==========
    // NOTE: Lấy danh sách tất cả nhân viên để chọn khi tạo/cập nhật station
    @GetMapping("/staff/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StaffSummaryResponse>> getAllStaff() {
        log.info("Admin fetching all staff for station assignment");
        return ApiResponse.<List<StaffSummaryResponse>>builder()
                .result(stationService.getAllStaff())
                .build();
    }

    // Tạo trạm sạc mới
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> createStation(@Valid @RequestBody StationCreationRequest request) {
        log.info("Admin creating new station: {}", request.getName());
        return ApiResponse.<StationResponse>builder()
                .result(stationService.createStation(request))
                .build();
    }

    // NOTE: Cập nhật thông tin trạm sạc (name, address, operatorName, contactPhone, status)
    @PutMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StationResponse> updateStation(@PathVariable String stationId, @Valid @RequestBody StationUpdateRequest request) {
        log.info("Admin updating station: {}", stationId);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStation(stationId, request))
                .build();
    }

    // NOTE: Xóa trạm sạc theo id. Các charging points liên quan sẽ tự động bị xóa (cascade)
    @DeleteMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteStation(@PathVariable String stationId) {
        log.info("Admin deleting station: {}", stationId);
        stationService.deleteStation(stationId);
        return ApiResponse.<Void>builder()
                .message("Station deleted successfully")
                .build();
    }

    // ========== CHARGING POINTS ==========
    // NOTE: Tạo thêm trụ sạc cho trạm sạc đã tồn tại
    @PostMapping("/{stationId}/charging-points")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ChargingPointResponse> addChargingPointToStation(
            @PathVariable String stationId,
            @Valid @RequestBody ChargingPointCreationRequest request) {
        log.info("Admin adding charging point to station {}", stationId);
        return ApiResponse.<ChargingPointResponse>builder()
                .result(stationService.addChargingPointToStation(stationId, request))
                .message("Charging point created successfully")
                .build();
    }

    // NOTE: Lấy danh sách tất cả trụ sạc của một trạm sạc
    @GetMapping("/{stationId}/charging-points")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ChargingPointResponse>> getChargingPointsByStation(@PathVariable String stationId) {
        log.info("Admin fetching charging points for station {}", stationId);
        return ApiResponse.<List<ChargingPointResponse>>builder()
                .result(stationService.getChargingPointsByStation(stationId))
                .build();
    }

    // NOTE: Cập nhật thông tin trụ sạc (status, price, power, connectorType)
    @PutMapping("/{stationId}/charging-points/{chargingPointId}")
//    @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ChargingPointResponse> updateChargingPoint(
            @PathVariable String stationId,
            @PathVariable String chargingPointId,
            @Valid @RequestBody ChargingPointUpdateRequest request) {
        log.info("Admin updating charging point: {} at station: {}", chargingPointId, stationId);
        return ApiResponse.<ChargingPointResponse>builder()
                .result(stationService.updateChargingPoint(stationId, chargingPointId, request))
                .build();
    }

    // NOTE: Xóa trụ sạc theo id
    @DeleteMapping("/{stationId}/charging-points/{chargingPointId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteChargingPoint(
            @PathVariable String stationId,
            @PathVariable String chargingPointId) {
        log.info("Admin deleting charging point: {} from station: {}", chargingPointId, stationId);
        stationService.deleteChargingPoint(stationId, chargingPointId);
        return ApiResponse.<Void>builder()
                .message("Charging point deleted successfully")
                .build();
    }

    // ========== PAYMENT HISTORY ==========
    /**
     * Lấy lịch sử thanh toán của một trạm sạc.
     * ADMIN: Có thể xem lịch sử của bất kỳ trạm nào.
     * STAFF: Chỉ được xem lịch sử của trạm mình quản lý (logic check trong service).
     *
     * @param stationId ID của trạm cần xem lịch sử
     * @param startDate Ngày bắt đầu filter (format: yyyy-MM-dd) (optional)
     * @param endDate Ngày kết thúc filter (format: yyyy-MM-dd) (optional)
     * @param paymentMethod Phương thức thanh toán (CASH, ZALOPAY) (optional)
     * @return Danh sách lịch sử thanh toán đã hoàn thành, sắp xếp theo thời gian mới nhất trước
     */
    @GetMapping("/{stationId}/payment-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<com.swp.evchargingstation.dto.response.PaymentHistoryResponse>> getPaymentHistory(
            @PathVariable String stationId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) com.swp.evchargingstation.entity.Payment.PaymentMethod paymentMethod) {

        log.info("Fetching payment history for station {} - startDate: {}, endDate: {}, paymentMethod: {}",
                stationId, startDate, endDate, paymentMethod);

        return ApiResponse.<List<com.swp.evchargingstation.dto.response.PaymentHistoryResponse>>builder()
                .result(stationService.getPaymentHistory(stationId, startDate, endDate, paymentMethod))
                .build();
    }
}
