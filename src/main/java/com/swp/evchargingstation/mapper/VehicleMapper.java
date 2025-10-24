package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    @Mapping(target = "ownerId", source = "owner.userId")
    VehicleResponse toVehicleResponse(Vehicle vehicle);
}
