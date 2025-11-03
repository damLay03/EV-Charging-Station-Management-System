package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.request.RoleAssignmentRequest;
import com.swp.evchargingstation.dto.response.AdminUserResponse;
import com.swp.evchargingstation.dto.response.DriverResponse;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Users", description = "API quản lý người dùng (driver, staff, admin)")
public class UserController {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserService userService;
//=====================================================DRIVER===========================================================
    @PostMapping("/register")
    @Operation(
            summary = "Đăng ký tài khoản driver mới",
            description = "Tạo một tài khoản driver mới với email và mật khẩu. Email phải duy nhất trong hệ thống"
    )
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @GetMapping("/driver/myInfo")
    @Operation(
            summary = "Lấy thông tin cá nhân của driver",
            description = "Trả về thông tin chi tiết của driver đang đăng nhập bao gồm tên, email, số điện thoại, địa chỉ, và ngày tham gia"
    )
    public ApiResponse<DriverResponse> getMyInfo() {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    // SELF update only (admin cũng KHÔNG can thiệp user khác)
    @PatchMapping("/driver/myInfo")
    @Operation(
            summary = "Cập nhật thông tin cá nhân của driver",
            description = "Driver cập nhật thông tin cá nhân của chính mình. Chỉ có thể sửa tên, số điện thoại, địa chỉ. Email và mật khẩu không thể sửa qua endpoint này"
    )
    public ApiResponse<DriverResponse> updateMyInfo(@RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

//=====================================================STAFF============================================================


//=====================================================ADMIN============================================================

    @GetMapping("/{userId}")
    @Operation(
            summary = "Lấy thông tin người dùng theo ID",
            description = "Trả về thông tin cơ bản của một người dùng theo ID. Bao gồm tên, email, và vai trò"
    )
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    // NOTE: ADMIN lấy thông tin đầy đủ của một driver (bao gồm address và joinDate)
    @GetMapping("/driver/{driverId}/info")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thông tin chi tiết driver (ADMIN only)",
            description = "ADMIN lấy thông tin đầy đủ của một driver bao gồm tên, email, số điện thoại, địa chỉ, và ngày tham gia. Chỉ quản trị viên có quyền truy cập"
    )
    public ApiResponse<DriverResponse> getDriverInfo(@PathVariable("driverId") String driverId) {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getDriverInfo(driverId))
                .build();
    }

    // NOTE: ADMIN cập nhật thông tin driver (không thể sửa email, password, joinDate)
    @PutMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật thông tin driver (ADMIN only)",
            description = "ADMIN cập nhật thông tin chi tiết của driver. Không thể sửa email, mật khẩu và ngày tham gia. Chỉ quản trị viên có quyền cập nhật"
    )
    public ApiResponse<DriverResponse> updateDriverByAdmin(@PathVariable("driverId") String driverId,
                                                           @RequestBody @Valid AdminUpdateDriverRequest request) {
        log.info("Admin updating driver: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateDriverByAdmin(driverId, request))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả driver",
            description = "Trả về danh sách tất cả các driver trong hệ thống với thông tin cơ bản. Dùng cho quản trị viên quản lý driver"
    )
    public ApiResponse<List<AdminUserResponse>> getDrivers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Authentication: {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        return ApiResponse.<List<AdminUserResponse>>builder()
                .result(userService.getDriversForAdmin())
                .build();
    }

    // ADMIN delete user by id (hard delete). Trả về code mặc định 1000 nếu thành công.
    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Xóa người dùng",
            description = "Xóa một người dùng khỏi hệ thống theo ID. Chỉ quản trị viên có quyền xóa. Đây là xóa vĩnh viễn (hard delete) không thể khôi phục"
    )
    public ApiResponse<Void> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder().message("Deleted").build();
    }

    // ADMIN assign role to a user
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Gán vai trò cho người dùng",
            description = "ADMIN gán một vai trò (DRIVER, STAFF, ADMIN) cho người dùng. Chỉ quản trị viên có quyền gán vai trò"
    )
    public ApiResponse<UserResponse> assignRole(@PathVariable("userId") String userId,
                                                @RequestBody @Valid RoleAssignmentRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.assignRoleToUser(userId, request))
                .build();
    }
}
