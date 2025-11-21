package com.swp.evchargingstation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingAvailabilityDto {
    private boolean available;
    private double maxChargePercentage;
    private String message;
}

