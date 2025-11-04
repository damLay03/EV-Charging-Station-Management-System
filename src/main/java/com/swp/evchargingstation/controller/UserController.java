package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
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
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Users", description = "API quản lý người dùng (driver, staff, admin)")
public class UserController {

    UserService userService;
    @PostMapping
    @Operation(
            summary = "Đăng ký tài khoản driver mới",
            description = "Tạo một tài khoản driver mới với email và mật khẩu. Email phải duy nhất trong hệ thống"
    )
    public ApiResponse<UserResponse> create(@RequestBody @Valid UserCreationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin cá nhân của chính mình",
            description = "Trả về thông tin chi tiết của user đang đăng nhập bao gồm tên, email, số điện thoại, địa chỉ, và ngày tham gia"
    )
    public ApiResponse<DriverResponse> get() {
        log.info("Fetching current user profile");
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật thông tin cá nhân",
            description = "User cập nhật thông tin cá nhân của chính mình. Chỉ có thể sửa tên, số điện thoại, địa chỉ. Email và mật khẩu không thể sửa qua endpoint này"
    )
    public ApiResponse<DriverResponse> update(@RequestBody @Valid UserUpdateRequest request) {
        log.info("Updating current user profile");
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }
}
