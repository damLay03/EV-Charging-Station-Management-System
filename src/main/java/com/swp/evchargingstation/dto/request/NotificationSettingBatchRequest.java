package com.swp.evchargingstation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationSettingBatchRequest {
    @NotEmpty(message = "Settings list cannot be empty")
    @Valid
    List<NotificationSettingRequest> settings;
}
