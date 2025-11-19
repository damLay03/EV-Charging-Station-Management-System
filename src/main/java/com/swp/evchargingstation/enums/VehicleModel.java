package com.swp.evchargingstation.enums;

import lombok.Getter;

@Getter
public enum VehicleModel {
    // VinFast Models
    // Replace placeholder URLs with your actual Cloudinary URLs
    VINFAST_VF5("VF5", VehicleBrand.VINFAST, 37.23f, "LFP (Lithium Iron Phosphate)", "100 kW", 100f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574643/vinfast-vf5_zkw6tn.png"),
    VINFAST_VF6("VF6", VehicleBrand.VINFAST, 59.6f, "LFP (Lithium Iron Phosphate)", "110 kW", 110f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574724/vinfast-vf6_fjt5ny.png"),
    VINFAST_VF7("VF7", VehicleBrand.VINFAST, 75.3f, "NMC (Nickel Manganese Cobalt)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574677/vinfast-vf7_gv08eq.png"),
    VINFAST_VF8("VF8", VehicleBrand.VINFAST, 87.7f, "NMC (Nickel Manganese Cobalt)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574650/vinfast-vf8_zsphes.png"),
    VINFAST_VF9("VF9", VehicleBrand.VINFAST, 123.0f, "NMC (Nickel Manganese Cobalt)", "188 kW", 188f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574661/vinfast-vf9_lrqgbl.png"),
    VINFAST_VFE34("VF e34", VehicleBrand.VINFAST, 42.0f, "LFP (Lithium Iron Phosphate)", "100 kW", 100f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574672/vinfast-vfe34_fef6iw.png"),

    // Tesla Models
    TESLA_MODEL_3("Model 3 Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574585/tesla-model-3_jyorat.png"),
    TESLA_MODEL_3_LONG_RANGE("Model 3 Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574623/tesla-model-3-long-range_xsxkf0.png"),
    TESLA_MODEL_Y("Model Y Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574630/tesla-model-y_w5ngnm.png"),
    TESLA_MODEL_Y_LONG_RANGE("Model Y Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763576708/tesla-model-y-long-range_sapnq6.png"),
    TESLA_MODEL_S("Model S", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763576577/tesla-model-s_otr5ig.png"),
    TESLA_MODEL_X("Model X", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574601/tesla-model-x_kmopai.png"),

    // BYD Models
    BYD_ATTO_3("Atto 3", VehicleBrand.BYD, 60.48f, "Blade Battery (LFP)", "88 kW", 88f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574685/byd-atto-3_ul7a1r.png"),
    BYD_DOLPHIN("Dolphin", VehicleBrand.BYD, 44.9f, "Blade Battery (LFP)", "60 kW", 60f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574699/byd-dolphin_luf53b.png"),
    BYD_SEAL("Seal", VehicleBrand.BYD, 82.56f, "Blade Battery (LFP)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574494/byd-seal_pwmv5i.png"),
    BYD_HAN("Han EV", VehicleBrand.BYD, 85.44f, "Blade Battery (LFP)", "120 kW", 120f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574738/byd-han_vcxc2t.png"),
    BYD_TANG("Tang EV", VehicleBrand.BYD, 108.8f, "Blade Battery (LFP)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574613/byd-tang_f3mxhp.png"),
    BYD_YUAN_PLUS("Yuan Plus", VehicleBrand.BYD, 50.12f, "Blade Battery (LFP)", "70 kW", 70f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763574532/byd-yuan-plus_lqlhkm.png");

    private final String modelName;
    private final VehicleBrand brand;
    private final float batteryCapacityKwh;
    private final String batteryType;
    private final String maxChargingPower;
    private final float maxChargingPowerKw;
    private final String imageUrl;

    VehicleModel(String modelName, VehicleBrand brand, float batteryCapacityKwh, String batteryType, String maxChargingPower, float maxChargingPowerKw, String imageUrl) {
        this.modelName = modelName;
        this.brand = brand;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.batteryType = batteryType;
        this.maxChargingPower = maxChargingPower;
        this.maxChargingPowerKw = maxChargingPowerKw;
        this.imageUrl = imageUrl;
    }

    // Helper method để lấy tất cả models của một brand
    public static VehicleModel[] getModelsByBrand(VehicleBrand brand) {
        return java.util.Arrays.stream(values())
                .filter(model -> model.getBrand() == brand)
                .toArray(VehicleModel[]::new);
    }
}
