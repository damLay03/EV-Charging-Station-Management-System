package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.NotificationSetting;
import com.swp.evchargingstation.enums.NotificationChannel;
import com.swp.evchargingstation.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, String> {
    List<NotificationSetting> findByUser_UserId(String userId);

    Optional<NotificationSetting> findByUser_UserIdAndNotificationTypeAndChannel(
            String userId, NotificationType notificationType, NotificationChannel channel);

    void deleteByUser_UserId(String userId);
}

