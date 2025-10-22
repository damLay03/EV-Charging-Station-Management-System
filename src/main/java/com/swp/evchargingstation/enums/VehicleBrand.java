package com.swp.evchargingstation.enums;

import lombok.Getter;

@Getter
public enum VehicleBrand {
    VINFAST("VinFast", "Việt Nam"),
    TESLA("Tesla", "Mỹ"),
    BYD("BYD", "Trung Quốc");

    private final String displayName;
    private final String country;

    VehicleBrand(String displayName, String country) {
        this.displayName = displayName;
        this.country = country;
    }
}

