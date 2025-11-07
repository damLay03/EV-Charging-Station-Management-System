package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.service.StationService;
import com.swp.evchargingstation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Users Management", description = "RESTful API quản lý người dùng (Driver, Staff, Admin) - Phân quyền theo role")
public class UserController {

    UserService userService;
    StationService stationService;

    // ==================== PUBLIC ENDPOINTS ====================

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
            summary = "[PUBLIC] Đăng ký tài khoản driver mới",
            description = "Tạo một tài khoản driver mới với email và mật khẩu. Email phải duy nhất trong hệ thống"
    )
    public ApiResponse<UserResponse> create(@RequestBody @Valid UserCreationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    // ==================== AUTHENTICATED USER ENDPOINTS ====================

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "[ALL] Lấy thông tin cá nhân của chính mình",
            description = "Trả về thông tin chi tiết của user đang đăng nhập. " +
                    "Response sẽ khác nhau tùy theo role: " +
                    "DRIVER (address, joinDate), " +
                    "STAFF (employeeNo, position, station info), " +
                    "ADMIN (department)"
    )
    public ApiResponse<UserProfileResponse> getMyProfile() {
        log.info("Fetching current user profile");
        return ApiResponse.<UserProfileResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "[ALL] Cập nhật thông tin cá nhân",
            description = "User cập nhật thông tin cá nhân của chính mình. " +
                    "Có thể sửa: tên, số điện thoại, ngày sinh, giới tính. " +
                    "DRIVER có thể sửa thêm địa chỉ. " +
                    "Không thể sửa: email, mật khẩu, role"
    )
    public ApiResponse<UserProfileResponse> updateMyProfile(@RequestBody @Valid UserUpdateRequest request) {
        log.info("Updating current user profile");
        return ApiResponse.<UserProfileResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

    // ==================== ADMIN - DRIVER MANAGEMENT ====================

    @GetMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy danh sách tất cả driver",
            description = "Trả về danh sách tất cả các driver trong hệ thống với thông tin cơ bản"
    )
    public ApiResponse<List<AdminUserResponse>> getAllDrivers() {
        log.info("Admin fetching all drivers");
        return ApiResponse.<List<AdminUserResponse>>builder()
                .result(userService.getDriversForAdmin())
                .build();
    }

    @GetMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy thông tin chi tiết driver",
            description = "ADMIN lấy thông tin đầy đủ của một driver bao gồm tên, email, số điện thoại, địa chỉ, và ngày tham gia"
    )
    public ApiResponse<DriverResponse> getDriverInfo(@PathVariable String driverId) {
        log.info("Admin fetching driver info: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getDriverInfo(driverId))
                .build();
    }

    @PutMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Cập nhật thông tin driver",
            description = "ADMIN cập nhật thông tin chi tiết của driver. Không thể sửa email, mật khẩu và ngày tham gia"
    )
    public ApiResponse<DriverResponse> updateDriver(@PathVariable String driverId,
                                                     @RequestBody @Valid AdminUpdateDriverRequest request) {
        log.info("Admin updating driver: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateDriverByAdmin(driverId, request))
                .build();
    }
    @ResponseStatus(HttpStatus.NO_CONTENT)

    @DeleteMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa driver",
            description = "Xóa một driver khỏi hệ thống theo ID. Đây là xóa vĩnh viễn (hard delete) không thể khôi phục"
    )
    public ApiResponse<Void> deleteDriver(@PathVariable String driverId) {
        log.info("Admin deleting driver: {}", driverId);
        userService.deleteUser(driverId);
        return ApiResponse.<Void>builder()
                .message("Driver deleted successfully")
                .build();
    }

    // ==================== ADMIN - STAFF MANAGEMENT ====================

    @GetMapping("/staffs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy danh sách tất cả nhân viên",
            description = "Trả về danh sách tất cả nhân viên để chọn khi tạo hoặc cập nhật trạm sạc"
    )
    public ApiResponse<List<StaffSummaryResponse>> getAllStaff() {
        log.info("Admin fetching all staff for station assignment");
        return ApiResponse.<List<StaffSummaryResponse>>builder()
                .result(stationService.getAllStaff())
                .build();
    }

    @GetMapping("/staffs/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Lấy thông tin chi tiết staff",
            description = "ADMIN lấy thông tin đầy đủ của một staff bao gồm tên, email, số điện thoại, chức vụ, mã nhân viên, và trạm quản lý"
    )
    public ApiResponse<StaffResponse> getStaffInfo(@PathVariable String staffId) {
        log.info("Admin fetching staff info: {}", staffId);
        return ApiResponse.<StaffResponse>builder()
                .result(userService.getStaffInfo(staffId))
                .build();
    }

    @PutMapping("/staffs/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Cập nhật thông tin staff",
            description = "ADMIN cập nhật thông tin chi tiết của staff. Không thể sửa email, mật khẩu và trạm quản lý (sửa trạm phải qua API station)"
    )
    public ApiResponse<StaffResponse> updateStaff(@PathVariable String staffId,
                                                   @RequestBody @Valid com.swp.evchargingstation.dto.request.AdminUpdateStaffRequest request) {
        log.info("Admin updating staff: {}", staffId);
        return ApiResponse.<StaffResponse>builder()
                .result(userService.updateStaffByAdmin(staffId, request))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/staffs/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[ADMIN] Xóa staff",
            description = "Xóa một staff khỏi hệ thống theo ID. Đây là xóa vĩnh viễn (hard delete) không thể khôi phục"
    )
    public ApiResponse<Void> deleteStaff(@PathVariable String staffId) {
        log.info("Admin deleting staff: {}", staffId);
        userService.deleteUser(staffId);
        return ApiResponse.<Void>builder()
                .message("Staff deleted successfully")
                .build();
    }
}

