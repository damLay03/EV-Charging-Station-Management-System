package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.NotificationSettingBatchRequest;
import com.swp.evchargingstation.dto.request.NotificationSettingRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.NotificationSettingResponse;
import com.swp.evchargingstation.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Notification Settings", description = "API quản lý cài đặt thông báo của người dùng")
public class NotificationSettingController {
    NotificationSettingService notificationSettingService;

    // NOTE: Lấy tất cả cài đặt thông báo của user hiện tại
    @GetMapping
    @Operation(
            summary = "Lấy tất cả cài đặt thông báo",
            description = "Trả về danh sách tất cả cài đặt thông báo của người dùng hiện tại bao gồm các loại thông báo và trạng thái bật/tắt của chúng"
    )
    public ApiResponse<List<NotificationSettingResponse>> getMyNotificationSettings() {
        return ApiResponse.<List<NotificationSettingResponse>>builder()
                .result(notificationSettingService.getMyNotificationSettings())
                .build();
    }

    // NOTE: Cập nhật batch nhiều cài đặt thông báo cùng lúc (khi nhấn "Lưu cài đặt")
    @PutMapping
    @Operation(
            summary = "Cập nhật nhiều cài đặt thông báo cùng lúc",
            description = "Cập nhật batch nhiều cài đặt thông báo của người dùng cùng một lúc. Sử dụng khi người dùng nhấn nút 'Lưu cài đặt' sau khi thay đổi nhiều cài đặt"
    )
    public ApiResponse<List<NotificationSettingResponse>> updateMyNotificationSettings(
            @RequestBody @Valid NotificationSettingBatchRequest request) {
        return ApiResponse.<List<NotificationSettingResponse>>builder()
                .result(notificationSettingService.updateMyNotificationSettings(request))
                .build();
    }

    // NOTE: Cập nhật một cài đặt cụ thể (khi toggle từng switch)
    @PatchMapping("/single")
    @Operation(
            summary = "Cập nhật một cài đặt thông báo cụ thể",
            description = "Cập nhật một cài đặt thông báo cụ thể của người dùng. Sử dụng khi người dùng bật/tắt từng switch cài đặt individual"
    )
    public ApiResponse<NotificationSettingResponse> updateSingleSetting(
            @RequestBody @Valid NotificationSettingRequest request) {
        return ApiResponse.<NotificationSettingResponse>builder()
                .result(notificationSettingService.updateSingleSetting(request))
                .build();
    }
}

