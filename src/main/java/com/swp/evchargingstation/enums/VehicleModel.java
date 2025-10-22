package com.swp.evchargingstation.enums;

import lombok.Getter;

@Getter
public enum VehicleModel {
    // VinFast Models
    VINFAST_VF5("VF5", VehicleBrand.VINFAST, 37.23f, "LFP (Lithium Iron Phosphate)"),
    VINFAST_VF6("VF6", VehicleBrand.VINFAST, 59.6f, "LFP (Lithium Iron Phosphate)"),
    VINFAST_VF7("VF7", VehicleBrand.VINFAST, 75.3f, "NMC (Nickel Manganese Cobalt)"),
    VINFAST_VF8("VF8", VehicleBrand.VINFAST, 87.7f, "NMC (Nickel Manganese Cobalt)"),
    VINFAST_VF9("VF9", VehicleBrand.VINFAST, 123.0f, "NMC (Nickel Manganese Cobalt)"),
    VINFAST_VFE34("VF e34", VehicleBrand.VINFAST, 42.0f, "LFP (Lithium Iron Phosphate)"),

    // Tesla Models
    TESLA_MODEL_3("Model 3 Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)"),
    TESLA_MODEL_3_LONG_RANGE("Model 3 Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)"),
    TESLA_MODEL_Y("Model Y Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)"),
    TESLA_MODEL_Y_LONG_RANGE("Model Y Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)"),
    TESLA_MODEL_S("Model S", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)"),
    TESLA_MODEL_X("Model X", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)"),

    // BYD Models
    BYD_ATTO_3("Atto 3", VehicleBrand.BYD, 60.48f, "Blade Battery (LFP)"),
    BYD_DOLPHIN("Dolphin", VehicleBrand.BYD, 44.9f, "Blade Battery (LFP)"),
    BYD_SEAL("Seal", VehicleBrand.BYD, 82.56f, "Blade Battery (LFP)"),
    BYD_HAN("Han EV", VehicleBrand.BYD, 85.44f, "Blade Battery (LFP)"),
    BYD_TANG("Tang EV", VehicleBrand.BYD, 108.8f, "Blade Battery (LFP)"),
    BYD_YUAN_PLUS("Yuan Plus", VehicleBrand.BYD, 50.12f, "Blade Battery (LFP)");

    private final String modelName;
    private final VehicleBrand brand;
    private final float batteryCapacityKwh;
    private final String batteryType;

    VehicleModel(String modelName, VehicleBrand brand, float batteryCapacityKwh, String batteryType) {
        this.modelName = modelName;
        this.brand = brand;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.batteryType = batteryType;
    }

    // Helper method để lấy tất cả models của một brand
    public static VehicleModel[] getModelsByBrand(VehicleBrand brand) {
        return java.util.Arrays.stream(values())
                .filter(model -> model.getBrand() == brand)
                .toArray(VehicleModel[]::new);
    }
}

