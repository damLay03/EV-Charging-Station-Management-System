package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "brandDisplayName", expression = "java(vehicle.getBrand().getDisplayName())")
    @Mapping(target = "modelName", expression = "java(vehicle.getModel().getModelName())")
    VehicleResponse toVehicleResponse(Vehicle vehicle);
}
