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
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Stations", description = "API quản lý trạm sạc và trụ sạc (charging points)")
public class StationController {
    StationService stationService;

    // Endpoint overview: trả về tất cả trạm + cờ active cho FE (nhẹ hơn so với full StationResponse)
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách trạm sạc overview",
            description = "Trả về danh sách tất cả các trạm sạc với thông tin cơ bản nhẹ hơn. Dùng cho dashboard quản trị để hiển thị tổng quan các trạm"
    )
    public ApiResponse<List<StationOverviewResponse>> getOverview() {
        log.info("Admin fetching station overview");
        return ApiResponse.<List<StationOverviewResponse>>builder()
                .result(stationService.getAllOverview())
                .build();
    }

    // Danh sách trạm với thông tin cơ bản
    @GetMapping
    @Operation(
            summary = "Lấy danh sách trạm sạc",
            description = "Trả về danh sách các trạm sạc với thông tin cơ bản. Có thể lọc theo trạng thái (OPERATIONAL, OUT_OF_SERVICE, MAINTENANCE, CLOSED)"
    )
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
    @Operation(
            summary = "Lấy danh sách trạm sạc chi tiết",
            description = "Trả về danh sách trạm sạc với thông tin đầy đủ bao gồm danh sách trụ sạc, doanh thu, tỷ lệ sử dụng, và nhân viên quản lý. Dùng cho giao diện quản lý trạm"
    )
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
    @Operation(
            summary = "Cập nhật trạng thái trạm sạc",
            description = "Cập nhật trạng thái của trạm sạc. Các trạng thái có thể là: OPERATIONAL (hoạt động), OUT_OF_SERVICE (tạm dừng), MAINTENANCE (bảo trì), CLOSED (đóng cửa)"
    )
    public ApiResponse<StationResponse> updateStationStatus(@PathVariable String stationId, @RequestParam StationStatus status) {
        log.info("Admin updating station {} to status {}", stationId, status);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStationStatus(stationId, status))
                .build();
    }

//    // Đặt trạng thái hoạt động (OPERATIONAL)
//    @PatchMapping("/{stationId}/activate")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<StationResponse> activate(@PathVariable String stationId) {
//        return ApiResponse.<StationResponse>builder()
//                .result(stationService.activate(stationId))
//                .build();
//    }
//
//    // Đặt trạng thái OUT_OF_SERVICE (ngưng hoạt động)
//    @PatchMapping("/{stationId}/deactivate")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<StationResponse> deactivate(@PathVariable String stationId) {
//        return ApiResponse.<StationResponse>builder()
//                .result(stationService.deactivate(stationId))
//                .build();
//    }
//
//    // Toggle giữa OPERATIONAL và OUT_OF_SERVICE
//    @PatchMapping("/{stationId}/toggle")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<StationResponse> toggle(@PathVariable String stationId) {
//        return ApiResponse.<StationResponse>builder()
//                .result(stationService.toggle(stationId))
//                .build();
//    }

    // ========== STAFF ==========
    // NOTE: Lấy danh sách tất cả nhân viên để chọn khi tạo/cập nhật station
    @GetMapping("/staff/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách tất cả nhân viên",
            description = "Trả về danh sách tất cả nhân viên để chọn khi tạo hoặc cập nhật trạm sạc. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<List<StaffSummaryResponse>> getAllStaff() {
        log.info("Admin fetching all staff for station assignment");
        return ApiResponse.<List<StaffSummaryResponse>>builder()
                .result(stationService.getAllStaff())
                .build();
    }

    // Tạo trạm sạc mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo trạm sạc mới",
            description = "Tạo một trạm sạc mới với các thông tin cơ bản như tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ. Chỉ quản trị viên có quyền tạo"
    )
    public ApiResponse<StationResponse> createStation(@Valid @RequestBody StationCreationRequest request) {
        log.info("Admin creating new station: {}", request.getName());
        return ApiResponse.<StationResponse>builder()
                .result(stationService.createStation(request))
                .build();
    }

    // NOTE: Cập nhật thông tin trạm sạc (name, address, operatorName, contactPhone, status)
    @PutMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật thông tin trạm sạc",
            description = "Cập nhật thông tin chi tiết của trạm sạc bao gồm tên, địa chỉ, tên nhà điều hành, số điện thoại liên hệ, và trạng thái. Chỉ quản trị viên có quyền cập nhật"
    )
    public ApiResponse<StationResponse> updateStation(@PathVariable String stationId, @Valid @RequestBody StationUpdateRequest request) {
        log.info("Admin updating station: {}", stationId);
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStation(stationId, request))
                .build();
    }

    // NOTE: Xóa trạm sạc theo id. Các charging points liên quan sẽ tự động bị xóa (cascade)
    @DeleteMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa trạm sạc",
            description = "Xóa một trạm sạc khỏi hệ thống theo ID. Các trụ sạc liên quan sẽ tự động bị xóa (cascade delete). Chỉ quản trị viên có quyền xóa"
    )
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
    @Operation(
            summary = "Thêm trụ sạc mới vào trạm sạc",
            description = "Tạo thêm một trụ sạc mới cho trạm sạc đã tồn tại. Bao gồm các thông tin như loại đầu nối, công suất, giá cước. Chỉ quản trị viên có quyền thêm"
    )
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
    @Operation(
            summary = "Lấy danh sách trụ sạc của một trạm",
            description = "Trả về danh sách tất cả các trụ sạc thuộc một trạm sạc cụ thể, bao gồm thông tin trạng thái, giá cước, công suất, loại đầu nối"
    )
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
    @Operation(
            summary = "Cập nhật thông tin trụ sạc",
            description = "Cập nhật thông tin chi tiết của trụ sạc bao gồm trạng thái, giá cước, công suất, loại đầu nối. Admin và nhân viên trạm có quyền cập nhật"
    )
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
    @Operation(
            summary = "Xóa trụ sạc",
            description = "Xóa một trụ sạc khỏi trạm sạc theo ID. Chỉ quản trị viên có quyền xóa. Trụ sạc đã xóa sẽ không thể được sử dụng nữa"
    )
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
    @Operation(
            summary = "Lấy lịch sử thanh toán của trạm sạc",
            description = "Trả về lịch sử thanh toán của một trạm sạc. ADMIN có thể xem lịch sử của bất kỳ trạm nào, STAFF chỉ xem trạm mình quản lý. Có thể lọc theo ngày và phương thức thanh toán"
    )
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
