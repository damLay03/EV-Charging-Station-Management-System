package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.NotificationChannel;
import com.swp.evchargingstation.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationSettingResponse {
    String settingId;
    NotificationType notificationType;
    NotificationChannel channel;
    boolean isEnabled;
}

