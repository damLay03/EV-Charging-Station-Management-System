package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.enums.VehicleBrand;
import com.swp.evchargingstation.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Vehicles Management", description = "RESTful API quản lý xe, hãng xe, mẫu xe - Phân quyền theo role")
public class VehicleController {
    VehicleService vehicleService;

    // ==================== PUBLIC - BRANDS & MODELS ====================

    @GetMapping("/brands")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách tất cả hãng xe",
            description = "Trả về danh sách tất cả các hãng xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleBrandResponse>> getAllBrands() {
        log.info("Fetching all vehicle brands");
        return ApiResponse.<List<VehicleBrandResponse>>builder()
                .result(vehicleService.getAllBrands())
                .build();
    }

    @GetMapping("/brands/{brand}/models")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách mẫu xe theo hãng",
            description = "Trả về danh sách tất cả các mẫu xe của một hãng cụ thể"
    )
    public ApiResponse<List<VehicleModelResponse>> getModelsByBrand(
            @Parameter(description = "Tên hãng xe", example = "TESLA")
            @PathVariable VehicleBrand brand) {
        log.info("Fetching models for brand: {}", brand);
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getModelsByBrand(brand))
                .build();
    }

    @GetMapping("/models")
    @Operation(
            summary = "[PUBLIC] Lấy danh sách tất cả mẫu xe",
            description = "Trả về danh sách tất cả các mẫu xe có sẵn trong hệ thống"
    )
    public ApiResponse<List<VehicleModelResponse>> getAllModels() {
        log.info("Fetching all vehicle models");
        return ApiResponse.<List<VehicleModelResponse>>builder()
                .result(vehicleService.getAllModels())
                .build();
    }

    // ==================== STAFF - VEHICLE LOOKUP ====================

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
            summary = "[STAFF] Tìm kiếm xe theo biển số",
            description = "Staff nhập biển số xe để lấy tất cả thông tin xe và chủ xe cần thiết để bắt đầu phiên sạc"
    )
    public ApiResponse<VehicleLookupResponse> lookupByLicensePlate(
            @Parameter(description = "Biển số xe", example = "30A-12345")
            @RequestParam String licensePlate) {
        log.info("Staff searching vehicle with license plate: {}", licensePlate);
        return ApiResponse.<VehicleLookupResponse>builder()
                .result(vehicleService.lookupVehicleByLicensePlate(licensePlate))
                .build();
    }

    // ==================== DRIVER ENDPOINTS ====================

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Tạo xe mới và upload 6 ảnh",
            description = "Driver điền form thông tin xe và upload 6 ảnh bắt buộc: " +
                    "1) Ảnh mặt trước cà vẹt, 2) Ảnh mặt sau cà vẹt, 3) Ảnh đầu xe, " +
                    "4) Ảnh thân xe bên trái, 5) Ảnh thân xe bên phải, 6) Ảnh đuôi xe. " +
                    "Backend sẽ tự động upload tất cả ảnh lên Cloudinary và tạo xe với status PENDING chờ admin phê duyệt."
    )
    public ApiResponse<VehicleResponse> createVehicle(
            @Parameter(description = "Mẫu xe (VD: TESLA_MODEL_3, VINFAST_VF8)", required = true, example = "TESLA_MODEL_3")
            @RequestParam("model") String model,

            @Parameter(description = "Biển số xe", required = true, example = "30A-12345")
            @RequestParam("licensePlate") String licensePlate,

            @Parameter(description = "Ảnh mặt trước giấy đăng ký xe - cà vẹt (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("documentFrontImage") org.springframework.web.multipart.MultipartFile documentFrontImage,

            @Parameter(description = "Ảnh mặt sau giấy đăng ký xe - cà vẹt (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("documentBackImage") org.springframework.web.multipart.MultipartFile documentBackImage,

            @Parameter(description = "Ảnh đầu xe (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("frontImage") org.springframework.web.multipart.MultipartFile frontImage,

            @Parameter(description = "Ảnh thân xe - bên hông trái (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("sideLeftImage") org.springframework.web.multipart.MultipartFile sideLeftImage,

            @Parameter(description = "Ảnh thân xe - bên hông phải (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("sideRightImage") org.springframework.web.multipart.MultipartFile sideRightImage,

            @Parameter(description = "Ảnh đuôi xe (jpg, jpeg, png, max 5MB)", required = true)
            @RequestParam("rearImage") org.springframework.web.multipart.MultipartFile rearImage) {

        log.info("Driver creating vehicle with license plate: {}, model: {} (6 images)",
                licensePlate, model);

        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.createVehicleWithDocument(model, licensePlate,
                        documentFrontImage, documentBackImage, frontImage,
                        sideLeftImage, sideRightImage, rearImage))
                .message("Vehicle registration submitted successfully with 6 images. Please wait for admin approval.")
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Lấy danh sách xe đã được phê duyệt",
            description = "Trả về danh sách các xe có status APPROVED - đây là những xe driver có thể sử dụng để sạc"
    )
    public ApiResponse<List<VehicleResponse>> getMyVehicles() {
        log.info("Driver fetching their approved vehicles");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getMyApprovedVehicles())
                .build();
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Xem tất cả yêu cầu đăng ký xe",
            description = "Driver xem tất cả yêu cầu đăng ký xe của mình bao gồm: " +
                    "PENDING (đang chờ phê duyệt), APPROVED (đã phê duyệt), REJECTED (bị từ chối). " +
                    "Nếu bị từ chối, driver sẽ thấy rejectionReason để biết lý do và có thể nộp lại đơn."
    )
    public ApiResponse<List<VehicleResponse>> getMyVehicleRequests() {
        log.info("Driver fetching all their vehicle registration requests");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getMyAllVehicleRequests())
                .build();
    }

    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Lấy chi tiết xe của driver",
            description = "Trả về thông tin chi tiết của một xe cụ thể mà driver sở hữu, bao gồm tất cả thông tin hãng, mẫu, biển số và năm sản xuất"
    )
    public ApiResponse<VehicleResponse> getVehicleById(@PathVariable String vehicleId) {
        log.info("Driver fetching vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.getMyVehicle(vehicleId))
                .build();
    }

    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Cập nhật thông tin xe",
            description = "Driver cập nhật thông tin xe của chính mình bao gồm hãng xe, mẫu xe, biển số xe, và năm sản xuất. Driver chỉ có thể cập nhật xe của mình"
    )
    public ApiResponse<VehicleResponse> updateVehicle(@PathVariable String vehicleId,
                                               @RequestBody @Valid VehicleUpdateRequest request) {
        log.info("Driver updating vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .result(vehicleService.updateMyVehicle(vehicleId, request))
                .build();
    }
    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "[DRIVER] Xóa xe",
            description = "Driver xóa một xe của chính mình khỏi hệ thống. Driver chỉ có thể xóa xe của mình"
    )
    public ApiResponse<Void> deleteVehicle(@PathVariable String vehicleId) {
        log.info("Driver deleting vehicle: {}", vehicleId);
        vehicleService.deleteMyVehicle(vehicleId);
        return ApiResponse.<Void>builder()
                .message("Vehicle deleted successfully")
                .build();
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy danh sách xe của driver",
            description = "Admin lấy danh sách tất cả các xe của một driver cụ thể"
    )
    public ApiResponse<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        log.info("Admin fetching vehicles of driver: {}", driverId);
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getVehiclesByDriver(driverId))
                .build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy danh sách xe chờ phê duyệt",
            description = "Admin xem tất cả các xe đang chờ phê duyệt (status = PENDING)"
    )
    public ApiResponse<List<VehicleResponse>> getPendingVehicles() {
        log.info("Admin fetching pending vehicle registrations");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getPendingVehicles())
                .build();
    }

    @GetMapping("/all-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy tất cả xe với trạng thái",
            description = "Admin xem tất cả các xe trong hệ thống kèm trạng thái phê duyệt"
    )
    public ApiResponse<List<VehicleResponse>> getAllVehiclesWithStatus() {
        log.info("Admin fetching all vehicles with status");
        return ApiResponse.<List<VehicleResponse>>builder()
                .result(vehicleService.getAllVehiclesWithStatus())
                .build();
    }

    @PutMapping("/{vehicleId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Phê duyệt đăng ký xe",
            description = "Admin phê duyệt đơn đăng ký xe của driver. Xe được phê duyệt mới có thể sử dụng để sạc."
    )
    public ApiResponse<VehicleResponse> approveVehicle(@PathVariable String vehicleId) {
        log.info("Admin approving vehicle: {}", vehicleId);
        return ApiResponse.<VehicleResponse>builder()
                .message("Vehicle approved successfully")
                .result(vehicleService.approveVehicle(vehicleId))
                .build();
    }

    @PutMapping("/{vehicleId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Từ chối đăng ký xe",
            description = "Admin từ chối đơn đăng ký xe của driver với lý do cụ thể. Driver có thể nộp lại đơn sau khi sửa."
    )
    public ApiResponse<VehicleResponse> rejectVehicle(
            @PathVariable String vehicleId,
            @Parameter(description = "Lý do từ chối", example = "Giấy tờ xe không rõ ràng")
            @RequestParam String rejectionReason) {
        log.info("Admin rejecting vehicle: {} with reason: {}", vehicleId, rejectionReason);
        return ApiResponse.<VehicleResponse>builder()
                .message("Vehicle rejected")
                .result(vehicleService.rejectVehicle(vehicleId, rejectionReason))
                .build();
    }
}
