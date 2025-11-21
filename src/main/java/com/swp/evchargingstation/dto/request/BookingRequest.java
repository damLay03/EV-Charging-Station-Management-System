package com.swp.evchargingstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;

    @NotBlank(message = "Charging point ID is required")
    private String chargingPointId;

    @NotNull(message = "Booking time is required")
    @Future(message = "Booking time must be in the future")
    private LocalDateTime bookingTime;

    @NotNull(message = "Desired percentage is required")
    @Min(value = 1, message = "Minimum charge percentage is 1%")
    @Max(value = 100, message = "Maximum charge percentage is 100%")
    private Float desiredPercentage;
}

