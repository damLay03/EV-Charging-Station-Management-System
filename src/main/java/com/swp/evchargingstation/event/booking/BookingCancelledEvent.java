package com.swp.evchargingstation.event.booking;

import com.swp.evchargingstation.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi user cancel booking.
 *
 * Use cases:
 * - NO REFUND - Deposit bị forfeit theo policy
 * - Gửi email thông báo cancellation
 * - Release charging point reservation
 *
 * Listeners:
 * - EmailNotificationListener: Gửi email cancellation (ASYNC)
 * - ChargingPointStatusListener: Release reservation (SYNC)
 *
 * IMPORTANT:
 * - Deposit KHÔNG được hoàn lại (policy)
 * - Booking status đã = CANCELLED_BY_USER trước khi event được publish
 */
@Getter
public class BookingCancelledEvent extends ApplicationEvent {

    private final Booking booking;
    private final Long bookingId;
    private final String userId;
    private final String chargingPointId;
    private final double depositAmount;

    public BookingCancelledEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
        this.bookingId = booking.getId();
        this.userId = booking.getUser().getUserId();
        this.chargingPointId = booking.getChargingPoint().getPointId();
        this.depositAmount = booking.getDepositAmount() != null ? booking.getDepositAmount() : 0.0;
    }
}

