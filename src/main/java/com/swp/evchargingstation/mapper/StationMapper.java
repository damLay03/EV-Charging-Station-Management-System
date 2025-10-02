package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.StationOverviewResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationMapper {
    @Mapping(target = "active", expression = "java(station.getStatus() == com.swp.evchargingstation.enums.StationStatus.OPERATIONAL)")
    StationResponse toStationResponse(Station station);

    @Mapping(target = "active", expression = "java(station.getStatus() == com.swp.evchargingstation.enums.StationStatus.OPERATIONAL)")
    StationOverviewResponse toStationOverviewResponse(Station station);
}
