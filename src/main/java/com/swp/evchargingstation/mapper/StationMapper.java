package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.StationOverviewResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationMapper {
    @Mapping(target = "active", expression = "java(station.getStatus() == com.swp.evchargingstation.enums.StationStatus.OPERATIONAL)")
    @Mapping(target = "staffId", expression = "java(station.getStaff() != null ? station.getStaff().getUserId() : null)")
    @Mapping(target = "staffName", expression = "java(station.getStaff() != null && station.getStaff().getUser() != null ? station.getStaff().getUser().getFullName() : null)")
    StationResponse toStationResponse(Station station);

    @Mapping(target = "active", expression = "java(station.getStatus() == com.swp.evchargingstation.enums.StationStatus.OPERATIONAL)")
    StationOverviewResponse toStationOverviewResponse(Station station);
}
