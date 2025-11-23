package com.swp.evchargingstation.event.session;

import com.swp.evchargingstation.entity.ChargingSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi charging session hoàn tất (COMPLETED).
 *
 * Use cases:
 * - Settle payment (wallet debit + refund deposit nếu có)
 * - Gửi email/SMS hoàn tất + hóa đơn
 * - Update booking status to COMPLETED
 * - Track analytics/revenue
 *
 * Listeners:
 * - PaymentSettlementListener: Xử lý thanh toán (SYNC, REQUIRES_NEW)
 * - EmailNotificationListener: Gửi email hoàn tất (ASYNC)
 * - BookingCompletionListener: Mark booking as completed (SYNC)
 * - AnalyticsListener: Track revenue/usage (ASYNC)
 *
 * ⚠️ IMPORTANT:
 * - Session đã COMPLETED trong DB trước khi event được publish
 * - Payment failure không affect session status (session vẫn COMPLETED)
 * - Email failure không affect payment (best effort notification)
 */
@Getter
public class ChargingSessionCompletedEvent extends ApplicationEvent {

    private final ChargingSession session;
    private final String sessionId;
    private final String driverId;
    private final String chargingPointId;
    private final float totalCost;
    private final float energyKwh;
    private final float durationMin;

    public ChargingSessionCompletedEvent(Object source, ChargingSession session) {
        super(source);
        this.session = session;
        this.sessionId = session.getSessionId();
        this.driverId = session.getDriver().getUserId();
        this.chargingPointId = session.getChargingPoint().getPointId();
        this.totalCost = session.getCostTotal();
        this.energyKwh = session.getEnergyKwh();
        this.durationMin = session.getDurationMin();
    }
}

