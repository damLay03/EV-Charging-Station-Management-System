package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.IncidentResponse;
import com.swp.evchargingstation.dto.response.StaffChargingPointResponse;
import com.swp.evchargingstation.dto.response.StaffTransactionResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Incident;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffDashboardMapper {

    // Map ChargingPoint to StaffChargingPointResponse
    @Mapping(target = "currentSessionId", expression = "java(chargingPoint.getCurrentSession() != null ? chargingPoint.getCurrentSession().getSessionId() : null)")
    @Mapping(target = "driverName", expression = "java(chargingPoint.getCurrentSession() != null && chargingPoint.getCurrentSession().getDriver() != null && chargingPoint.getCurrentSession().getDriver().getUser() != null ? chargingPoint.getCurrentSession().getDriver().getUser().getFullName() : null)")
    @Mapping(target = "vehicleModel", expression = "java(chargingPoint.getCurrentSession() != null && chargingPoint.getCurrentSession().getVehicle() != null ? chargingPoint.getCurrentSession().getVehicle().getModel() : null)")
    @Mapping(target = "startTime", expression = "java(chargingPoint.getCurrentSession() != null && chargingPoint.getCurrentSession().getStartTime() != null ? chargingPoint.getCurrentSession().getStartTime().format(java.time.format.DateTimeFormatter.ofPattern(\"HH:mm dd/MM/yyyy\")) : null)")
    @Mapping(target = "currentSocPercent", expression = "java(chargingPoint.getCurrentSession() != null ? chargingPoint.getCurrentSession().getEndSocPercent() : 0)")
    StaffChargingPointResponse toStaffChargingPointResponse(ChargingPoint chargingPoint);

    // Map ChargingSession to StaffTransactionResponse
    @Mapping(target = "driverName", expression = "java(session.getDriver() != null && session.getDriver().getUser() != null ? session.getDriver().getUser().getFullName() : \"N/A\")")
    @Mapping(target = "driverPhone", expression = "java(session.getDriver() != null && session.getDriver().getUser() != null ? session.getDriver().getUser().getPhone() : \"N/A\")")
    @Mapping(target = "vehicleModel", expression = "java(session.getVehicle() != null ? session.getVehicle().getModel() : \"N/A\")")
    @Mapping(target = "chargingPointId", expression = "java(session.getChargingPoint() != null ? session.getChargingPoint().getPointId() : \"N/A\")")
    @Mapping(target = "isPaid", ignore = true) // Will be set manually in service
    StaffTransactionResponse toStaffTransactionResponse(ChargingSession session);

    // Map Incident to IncidentResponse
    @Mapping(target = "reporterName", expression = "java(incident.getReporter() != null ? incident.getReporter().getFullName() : \"N/A\")")
    @Mapping(target = "stationName", expression = "java(incident.getStation() != null ? incident.getStation().getName() : \"N/A\")")
    @Mapping(target = "chargingPointId", expression = "java(incident.getChargingPoint() != null ? incident.getChargingPoint().getPointId() : null)")
    @Mapping(target = "assignedStaffName", expression = "java(incident.getAssignedStaff() != null && incident.getAssignedStaff().getUser() != null ? incident.getAssignedStaff().getUser().getFullName() : null)")
    IncidentResponse toIncidentResponse(Incident incident);
}

