package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.NotificationSettingResponse;
import com.swp.evchargingstation.entity.NotificationSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationSettingMapper {
    @Mapping(target = "isEnabled", source = "enabled")
    NotificationSettingResponse toNotificationSettingResponse(NotificationSetting notificationSetting);
}
