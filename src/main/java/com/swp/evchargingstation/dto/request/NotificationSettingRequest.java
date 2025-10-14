package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.NotificationChannel;
import com.swp.evchargingstation.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationSettingRequest {
    @NotNull(message = "Notification type is required")
    NotificationType notificationType;

    @NotNull(message = "Channel is required")
    NotificationChannel channel;

    @NotNull(message = "Enabled status is required")
    Boolean isEnabled;
}

