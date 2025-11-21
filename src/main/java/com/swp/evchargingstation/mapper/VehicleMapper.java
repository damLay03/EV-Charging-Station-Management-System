package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "ownerName", source = "owner.user.fullName")
    @Mapping(target = "ownerEmail", source = "owner.user.email")
    @Mapping(target = "ownerPhone", source = "owner.user.phone")
    @Mapping(target = "approvedByAdminId", source = "approvedBy.userId")
    @Mapping(target = "approvedByAdminName", source = "approvedBy.user.fullName")
    VehicleResponse toVehicleResponse(Vehicle vehicle);
}
