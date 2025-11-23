package com.swp.evchargingstation.event.session;

import com.swp.evchargingstation.entity.ChargingSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi charging session bắt đầu.
 *
 * Use cases:
 * - Gửi email/SMS thông báo cho driver
 * - Track analytics/metrics
 * - Update dashboard real-time
 *
 * Listeners:
 * - EmailNotificationListener: Gửi email thông báo (ASYNC)
 * - AnalyticsListener: Track session start metrics (ASYNC)
 */
@Getter
public class ChargingSessionStartedEvent extends ApplicationEvent {

    private final ChargingSession session;
    private final String sessionId;
    private final String driverId;
    private final String vehicleId;
    private final String chargingPointId;

    public ChargingSessionStartedEvent(Object source, ChargingSession session) {
        super(source);
        this.session = session;
        this.sessionId = session.getSessionId();
        this.driverId = session.getDriver().getUserId();
        this.vehicleId = session.getVehicle().getVehicleId();
        this.chargingPointId = session.getChargingPoint().getPointId();
    }
}


