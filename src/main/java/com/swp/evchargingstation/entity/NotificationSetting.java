package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.NotificationChannel;
import com.swp.evchargingstation.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notification_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "notification_type", "channel"})
})
public class NotificationSetting {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "setting_id")
    String settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    NotificationChannel channel;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    boolean isEnabled = true;
}

