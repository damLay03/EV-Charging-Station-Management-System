package com.swp.evchargingstation.enums;

import lombok.Getter;

@Getter
public enum VehicleModel {
    // VinFast Models
    // Replace placeholder URLs with your actual Cloudinary URLs
    VINFAST_VF5("VF5", VehicleBrand.VINFAST, 37.23f, "LFP (Lithium Iron Phosphate)", "100 kW", 100f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582872/vinfast-vf5_mlzwrj.png"),
    VINFAST_VF6("VF6", VehicleBrand.VINFAST, 59.6f, "LFP (Lithium Iron Phosphate)", "110 kW", 110f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582870/vinfast-vf6_qe9sfh.png"),
    VINFAST_VF7("VF7", VehicleBrand.VINFAST, 75.3f, "NMC (Nickel Manganese Cobalt)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582872/vinfast-vf7_oukh8t.png"),
    VINFAST_VF8("VF8", VehicleBrand.VINFAST, 87.7f, "NMC (Nickel Manganese Cobalt)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582882/vinfast-vf8_feorxz.png"),
    VINFAST_VF9("VF9", VehicleBrand.VINFAST, 123.0f, "NMC (Nickel Manganese Cobalt)", "188 kW", 188f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582872/vinfast-vf9_urwur4.png"),
    VINFAST_VFE34("VF e34", VehicleBrand.VINFAST, 42.0f, "LFP (Lithium Iron Phosphate)", "100 kW", 100f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763582867/vinfast-vfe34_c1jmdv.png"),

    // Tesla Models
    TESLA_MODEL_3("Model 3 Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619341/tesla-model-3_lc7n2r.jpg"),
    TESLA_MODEL_3_LONG_RANGE("Model 3 Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619339/tesla-model-3-long-range_ojpe6k.jpg"),
    TESLA_MODEL_Y("Model Y Standard Range", VehicleBrand.TESLA, 60.0f, "LFP (Lithium Iron Phosphate)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619339/tesla-model-y_y5qqbk.jpg"),
    TESLA_MODEL_Y_LONG_RANGE("Model Y Long Range", VehicleBrand.TESLA, 82.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619340/tesla-model-y-long-range_j43rfh.jpg"),
    TESLA_MODEL_S("Model S", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619339/tesla-model-s_vt71il.jpg"),
    TESLA_MODEL_X("Model X", VehicleBrand.TESLA, 100.0f, "NCA (Nickel Cobalt Aluminum)", "250 kW", 250f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619339/tesla-model-x_ewbfbz.jpg"),

    // BYD Models
    BYD_ATTO_3("Atto 3", VehicleBrand.BYD, 60.48f, "Blade Battery (LFP)", "88 kW", 88f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619340/byd-atto-3_cl4cbf.png"),
    BYD_DOLPHIN("Dolphin", VehicleBrand.BYD, 44.9f, "Blade Battery (LFP)", "60 kW", 60f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619339/byd-dolphin_usitn5.png"),
    BYD_SEAL("Seal", VehicleBrand.BYD, 82.56f, "Blade Battery (LFP)", "150 kW", 150f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619342/byd-seal_lyve7e.png"),
    BYD_HAN("Han EV", VehicleBrand.BYD, 85.44f, "Blade Battery (LFP)", "120 kW", 120f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619342/byd-han_haao4j.png"),
    BYD_TANG("Tang EV", VehicleBrand.BYD, 108.8f, "Blade Battery (LFP)", "170 kW", 170f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619342/byd-tang_lel6l0.png"),
    BYD_YUAN_PLUS("Yuan Plus", VehicleBrand.BYD, 50.12f, "Blade Battery (LFP)", "70 kW", 70f,
            "https://res.cloudinary.com/dppc2tng8/image/upload/v1763619342/byd-yuan-plus_hjevit.png");

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
