package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse {
    String subscriptionId;
    String driverId;
    PlanResponse plan;
    LocalDateTime startDate;
    LocalDateTime endDate;
    String status; // ACTIVE, EXPIRED, CANCELLED
    boolean autoRenew;
}

