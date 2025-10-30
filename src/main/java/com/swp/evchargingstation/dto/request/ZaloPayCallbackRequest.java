package com.swp.evchargingstation.dto.zalopay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZaloPayCallbackRequest {
    @JsonProperty("data")
    private String data;  // JSON string contains payment info

    @JsonProperty("mac")
    private String mac;   // HMAC for verification

    @JsonProperty("type")
    private int type;     // 1 = payment, 2 = agreement
}