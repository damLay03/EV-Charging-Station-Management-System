package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.ChargingPointResponse;
import com.swp.evchargingstation.entity.Booking;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.service.ChargingPointStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChargingPointMapper {

    private final ChargingPointStatusService chargingPointStatusService;

    public ChargingPointResponse toChargingPointResponse(ChargingPoint chargingPoint) {
        // Tính toán trạng thái hiển thị động
        var displayStatus = chargingPointStatusService.calculateDisplayStatus(chargingPoint.getPointId());

        // Lấy thông tin booking sắp tới (nếu có)
        Optional<Booking> upcomingBooking = chargingPointStatusService.getUpcomingBooking(chargingPoint.getPointId());

        var builder = ChargingPointResponse.builder()
                .pointId(chargingPoint.getPointId())
                .name(chargingPoint.getName())
                .stationId(chargingPoint.getStation() != null ? chargingPoint.getStation().getStationId() : null)
                .stationName(chargingPoint.getStation() != null ? chargingPoint.getStation().getName() : null)
                .chargingPower(chargingPoint.getChargingPower())
                .status(chargingPoint.getStatus()) // Trạng thái vật lý thực tế
                .displayStatus(displayStatus) // Trạng thái hiển thị động
                .currentSessionId(chargingPoint.getCurrentSession() != null ? chargingPoint.getCurrentSession().getSessionId() : null);

        // Thêm thông tin booking sắp tới nếu có
        if (upcomingBooking.isPresent()) {
            Booking booking = upcomingBooking.get();
            builder.upcomingBookingId(booking.getId())
                   .upcomingBookingTime(booking.getBookingTime())
                   .upcomingBookingUserName(booking.getUser() != null ? booking.getUser().getFullName() : null);
        }

        return builder.build();
    }
}

