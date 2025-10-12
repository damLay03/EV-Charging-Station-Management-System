package com.swp.evchargingstation.dto.request;

import com.swp.evchargingstation.enums.BillingType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanUpdateRequest {
    @NotBlank
    String name;

    @NotNull
    BillingType billingType;

    @PositiveOrZero
    float pricePerKwh;

    @PositiveOrZero
    float pricePerMinute;

    @PositiveOrZero
    float monthlyFee;

    @Size(max = 1000)
    String benefits;
}

