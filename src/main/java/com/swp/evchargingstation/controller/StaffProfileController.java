package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.StaffProfileResponse;
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

@RestController
@RequestMapping("/api/my-profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "My Profile", description = "Thông tin cá nhân của người dùng hiện tại")
public class StaffProfileController {

    StaffDashboardService staffDashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin hồ sơ cá nhân",
            description = "Trả về thông tin chi tiết của người dùng hiện tại (staff, driver, admin) bao gồm tên, email, số điện thoại, và các thông tin liên quan"
    )
    public ApiResponse<StaffProfileResponse> getMyProfile() {
        log.info("User requesting profile information");
        return ApiResponse.<StaffProfileResponse>builder()
                .result(staffDashboardService.getMyProfile())
                .build();
    }
}

