package com.swp.evchargingstation.event.booking;

import com.swp.evchargingstation.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi user check-in booking.
 *
 * Use cases:
 * - Gửi notification khi check-in thành công
 * - Update charging point status
 * - Track analytics
 *
 * Listeners:
 * - EmailNotificationListener: Gửi email check-in thành công (ASYNC)
 * - AnalyticsListener: Track check-in metrics (ASYNC)
 */
@Getter
public class BookingCheckedInEvent extends ApplicationEvent {

    private final Booking booking;
    private final Long bookingId;
    private final String userId;
    private final String chargingPointId;

    public BookingCheckedInEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
        this.bookingId = booking.getId();
        this.userId = booking.getUser().getUserId();
        this.chargingPointId = booking.getChargingPoint().getPointId();
    }
}

