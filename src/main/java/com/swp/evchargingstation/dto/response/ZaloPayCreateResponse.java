package com.swp.evchargingstation.dto.zalopay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZaloPayCreateResponse {
    @JsonProperty("return_code")
    private int returnCode;  // 1 = success, others = error

    @JsonProperty("return_message")
    private String returnMessage;

    @JsonProperty("sub_return_code")
    private int subReturnCode;

    @JsonProperty("sub_return_message")
    private String subReturnMessage;

    @JsonProperty("zp_trans_token")
    private String zpTransToken;  // Use this to redirect user

    @JsonProperty("order_url")
    private String orderUrl;

    @JsonProperty("order_token")
    private String orderToken;
}