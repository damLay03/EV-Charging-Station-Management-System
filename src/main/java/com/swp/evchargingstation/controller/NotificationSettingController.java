package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.NotificationSettingBatchRequest;
import com.swp.evchargingstation.dto.request.NotificationSettingRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.NotificationSettingResponse;
import com.swp.evchargingstation.service.NotificationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationSettingController {
    NotificationSettingService notificationSettingService;

    // NOTE: Lấy tất cả cài đặt thông báo của user hiện tại
    @GetMapping
    public ApiResponse<List<NotificationSettingResponse>> getMyNotificationSettings() {
        return ApiResponse.<List<NotificationSettingResponse>>builder()
                .result(notificationSettingService.getMyNotificationSettings())
                .build();
    }

    // NOTE: Cập nhật batch nhiều cài đặt thông báo cùng lúc (khi nhấn "Lưu cài đặt")
    @PutMapping
    public ApiResponse<List<NotificationSettingResponse>> updateMyNotificationSettings(
            @RequestBody @Valid NotificationSettingBatchRequest request) {
        return ApiResponse.<List<NotificationSettingResponse>>builder()
                .result(notificationSettingService.updateMyNotificationSettings(request))
                .build();
    }

    // NOTE: Cập nhật một cài đặt cụ thể (khi toggle từng switch)
    @PatchMapping("/single")
    public ApiResponse<NotificationSettingResponse> updateSingleSetting(
            @RequestBody @Valid NotificationSettingRequest request) {
        return ApiResponse.<NotificationSettingResponse>builder()
                .result(notificationSettingService.updateSingleSetting(request))
                .build();
    }
}

