package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.SubscriptionResponse;
import com.swp.evchargingstation.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PlanMapper.class})
public interface SubscriptionMapper {
    @Mapping(target = "driverId", source = "driver.userId")
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);
}

