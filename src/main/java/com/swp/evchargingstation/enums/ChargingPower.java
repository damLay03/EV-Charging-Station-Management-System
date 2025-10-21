package com.swp.evchargingstation.enums;

public enum ChargingPower {
    POWER_22KW(22.0f, "22kW - AC Charging"),
    POWER_50KW(50.0f, "50kW - DC Fast Charging"),
    POWER_120KW(120.0f, "120kW - DC Ultra Fast Charging"),
    POWER_350KW(350.0f, "350kW - DC Ultra Fast Charging");

    private final float powerKw;
    private final String description;

    ChargingPower(float powerKw, String description) {
        this.powerKw = powerKw;
        this.description = description;
    }

    public float getPowerKw() {
        return powerKw;
    }

    public String getDescription() {
        return description;
    }
}

