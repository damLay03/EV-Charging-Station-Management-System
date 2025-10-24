package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.ChargingPointResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import org.springframework.stereotype.Component;

@Component
public class ChargingPointMapper {
    public ChargingPointResponse toChargingPointResponse(ChargingPoint chargingPoint) {
        return ChargingPointResponse.builder()
                .pointId(chargingPoint.getPointId())
                .name(chargingPoint.getName())
                .stationId(chargingPoint.getStation() != null ? chargingPoint.getStation().getStationId() : null)
                .stationName(chargingPoint.getStation() != null ? chargingPoint.getStation().getName() : null)
                .chargingPower(chargingPoint.getChargingPower())
                .status(chargingPoint.getStatus())
                .currentSessionId(chargingPoint.getCurrentSession() != null ? chargingPoint.getCurrentSession().getSessionId() : null)
                .build();
    }
}
