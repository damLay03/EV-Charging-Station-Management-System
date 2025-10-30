package com.swp.evchargingstation.dto.zalopay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ZaloPayCreateRequest {
    @JsonProperty("app_id")
    private int appId;

    @JsonProperty("app_trans_id")
    private String appTransId;  // Format: yymmdd_xxxxx

    @JsonProperty("app_user")
    private String appUser;

    @JsonProperty("app_time")
    private long appTime;  // milliseconds

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("embed_data")
    private String embedData;  // JSON string

    @JsonProperty("item")
    private String item;  // JSON array string

    @JsonProperty("description")
    private String description;

    @JsonProperty("bank_code")
    private String bankCode;  // Optional, "" for all methods

    @JsonProperty("mac")
    private String mac;  // HMAC

    @JsonProperty("callback_url")
    private String callbackUrl;
}