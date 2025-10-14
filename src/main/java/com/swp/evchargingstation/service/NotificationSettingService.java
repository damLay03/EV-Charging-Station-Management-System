package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.NotificationSettingBatchRequest;
import com.swp.evchargingstation.dto.request.NotificationSettingRequest;
import com.swp.evchargingstation.dto.response.NotificationSettingResponse;
import com.swp.evchargingstation.entity.NotificationSetting;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.NotificationSettingMapper;
import com.swp.evchargingstation.repository.NotificationSettingRepository;
import com.swp.evchargingstation.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationSettingService {
    NotificationSettingRepository notificationSettingRepository;
    UserRepository userRepository;
    NotificationSettingMapper notificationSettingMapper;

    // NOTE: Lấy tất cả cài đặt thông báo của user hiện tại
    @Transactional(readOnly = true)
    public List<NotificationSettingResponse> getMyNotificationSettings() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("User '{}' fetching notification settings", user.getUserId());

        List<NotificationSetting> settings = notificationSettingRepository.findByUser_UserId(user.getUserId());
        return settings.stream()
                .map(notificationSettingMapper::toNotificationSettingResponse)
                .collect(Collectors.toList());
    }

    // NOTE: Cập nhật cài đặt thông báo (batch update)
    @Transactional
    public List<NotificationSettingResponse> updateMyNotificationSettings(NotificationSettingBatchRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("User '{}' updating notification settings", user.getUserId());

        // Xử lý từng setting trong request
        List<NotificationSetting> updatedSettings = request.getSettings().stream()
                .map(settingRequest -> {
                    // Tìm setting hiện có hoặc tạo mới
                    NotificationSetting setting = notificationSettingRepository
                            .findByUser_UserIdAndNotificationTypeAndChannel(
                                    user.getUserId(),
                                    settingRequest.getNotificationType(),
                                    settingRequest.getChannel()
                            )
                            .orElse(NotificationSetting.builder()
                                    .user(user)
                                    .notificationType(settingRequest.getNotificationType())
                                    .channel(settingRequest.getChannel())
                                    .build());

                    // Cập nhật trạng thái
                    setting.setEnabled(settingRequest.getIsEnabled());
                    return notificationSettingRepository.save(setting);
                })
                .collect(Collectors.toList());

        log.info("Updated {} notification settings for user '{}'", updatedSettings.size(), user.getUserId());

        return updatedSettings.stream()
                .map(notificationSettingMapper::toNotificationSettingResponse)
                .collect(Collectors.toList());
    }

    // NOTE: Cập nhật một cài đặt cụ thể
    @Transactional
    public NotificationSettingResponse updateSingleSetting(NotificationSettingRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("User '{}' updating single notification setting: {} - {}",
                user.getUserId(), request.getNotificationType(), request.getChannel());

        NotificationSetting setting = notificationSettingRepository
                .findByUser_UserIdAndNotificationTypeAndChannel(
                        user.getUserId(),
                        request.getNotificationType(),
                        request.getChannel()
                )
                .orElse(NotificationSetting.builder()
                        .user(user)
                        .notificationType(request.getNotificationType())
                        .channel(request.getChannel())
                        .build());

        setting.setEnabled(request.getIsEnabled());
        NotificationSetting saved = notificationSettingRepository.save(setting);

        log.info("Notification setting updated successfully");
        return notificationSettingMapper.toNotificationSettingResponse(saved);
    }

    // NOTE: Xóa tất cả cài đặt của user (admin only hoặc khi xóa tài khoản)
    @Transactional
    public void deleteUserSettings(String userId) {
        log.info("Deleting all notification settings for user '{}'", userId);
        notificationSettingRepository.deleteByUser_UserId(userId);
    }
}

