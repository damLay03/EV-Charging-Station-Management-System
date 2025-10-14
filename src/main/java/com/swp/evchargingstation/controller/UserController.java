package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.response.AdminUserResponse;
import com.swp.evchargingstation.dto.response.DriverResponse;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.service.UserService;
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
public class UserController {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserService userService;
//=====================================================DRIVER===========================================================
    @PostMapping("/register")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @GetMapping("/driver/myInfo")
    public ApiResponse<DriverResponse> getMyInfo() {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    // SELF update only (admin cũng KHÔNG can thiệp user khác)
    @PatchMapping("/driver/myInfo")
    public ApiResponse<DriverResponse> updateMyInfo(@RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

//=====================================================STAFF============================================================


//=====================================================ADMIN============================================================

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    // NOTE: ADMIN lấy thông tin đầy đủ của một driver (bao gồm address và joinDate)
    @GetMapping("/driver/{driverId}/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverResponse> getDriverInfo(@PathVariable("driverId") String driverId) {
        return ApiResponse.<DriverResponse>builder()
                .result(userService.getDriverInfo(driverId))
                .build();
    }

    // NOTE: ADMIN cập nhật thông tin driver (không thể sửa email, password, joinDate)
    @PutMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverResponse> updateDriverByAdmin(@PathVariable("driverId") String driverId,
                                                           @RequestBody @Valid AdminUpdateDriverRequest request) {
        log.info("Admin updating driver: {}", driverId);
        return ApiResponse.<DriverResponse>builder()
                .result(userService.updateDriverByAdmin(driverId, request))
                .build();
    }

    @GetMapping
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
    public ApiResponse<Void> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder().message("Deleted").build();
    }
}
