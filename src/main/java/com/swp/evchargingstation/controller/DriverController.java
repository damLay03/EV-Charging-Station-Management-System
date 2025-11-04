package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.response.AdminUserResponse;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.DriverResponse;
import com.swp.evchargingstation.service.UserService;
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
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Drivers", description = "API quản lý driver (ADMIN only)")
public class DriverController {

    UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách tất cả driver",
            description = "Trả về danh sách tất cả các driver trong hệ thống với thông tin cơ bản. Dùng cho quản trị viên quản lý driver"
    )
    public ApiResponse<List<AdminUserResponse>> getAllDrivers() {
        log.info("Admin fetching all drivers");
        return ApiResponse.<List<AdminUserResponse>>builder()
                .result(userService.getDriversForAdmin())
                .build();
    }

    @GetMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thông tin chi tiết driver",
            description = "ADMIN lấy thông tin đầy đủ của một driver bao gồm tên, email, số điện thoại, địa chỉ, và ngày tham gia. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<DriverResponse> getDriverInfo(@PathVariable String driverId) {
        log.info("Admin fetching driver info: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getDriverInfo(driverId))
                .build();
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật thông tin driver",
            description = "ADMIN cập nhật thông tin chi tiết của driver. Không thể sửa email, mật khẩu và ngày tham gia. Chỉ quản trị viên có quyền cập nhật"
    )
    public ApiResponse<DriverResponse> updateDriver(@PathVariable String driverId,
                                                     @RequestBody @Valid AdminUpdateDriverRequest request) {
        log.info("Admin updating driver: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateDriverByAdmin(driverId, request))
                .build();
    }

    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa driver",
            description = "Xóa một driver khỏi hệ thống theo ID. Chỉ quản trị viên có quyền xóa. Đây là xóa vĩnh viễn (hard delete) không thể khôi phục"
    )
    public ApiResponse<Void> deleteDriver(@PathVariable String driverId) {
        log.info("Admin deleting driver: {}", driverId);
        userService.deleteUser(driverId);
        return ApiResponse.<Void>builder()
                .message("Driver deleted successfully")
                .build();
    }
}

