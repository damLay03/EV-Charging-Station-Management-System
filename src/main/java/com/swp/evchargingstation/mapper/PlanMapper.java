package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.PlanResponse;
import com.swp.evchargingstation.entity.Plan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanMapper {
    PlanResponse toPlanResponse(Plan plan);
}

