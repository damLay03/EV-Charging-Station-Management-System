package com.swp.evchargingstation.event.booking;

import com.swp.evchargingstation.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi booking được tạo thành công.
 *
 * Use cases:
 * - Debit deposit từ wallet
 * - Gửi email xác nhận booking
 * - Update charging point reservation status
 *
 * Listeners:
 * - WalletDepositListener: Debit deposit từ wallet (SYNC, REQUIRES_NEW)
 * - EmailConfirmationListener: Gửi email xác nhận (ASYNC)
 *
 * ⚠️ IMPORTANT:
 * - Booking đã được save vào DB trước khi event được publish
 * - Deposit debit failure không rollback booking (separate transaction)
 * - User sẽ cần top-up nếu wallet insufficient
 */
@Getter
public class BookingCreatedEvent extends ApplicationEvent {

    private final Booking booking;
    private final Long bookingId;
    private final String userId;
    private final String chargingPointId;
    private final double depositAmount;

    public BookingCreatedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
        this.bookingId = booking.getId();
        this.userId = booking.getUser().getUserId();
        this.chargingPointId = booking.getChargingPoint().getPointId();
        this.depositAmount = booking.getDepositAmount() != null ? booking.getDepositAmount() : 0.0;
    }
}


