package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "staffId", source = "userId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", expression = "java(staff.getUser() == null ? null : staff.getUser().getFirstName() + \" \" + staff.getUser().getLastName())")
    @Mapping(target = "stationId", ignore = true)
    @Mapping(target = "stationName", ignore = true)
    StaffSummaryResponse toStaffSummaryResponse(Staff staff);
}
