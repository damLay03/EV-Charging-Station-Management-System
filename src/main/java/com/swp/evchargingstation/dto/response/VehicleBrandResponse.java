package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.VehicleBrand;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleBrandResponse {
    VehicleBrand brand;
    String displayName;
    String country;
}

