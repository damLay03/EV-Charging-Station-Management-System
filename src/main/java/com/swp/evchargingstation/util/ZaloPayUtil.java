package com.swp.evchargingstation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class ZaloPayUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate app_trans_id format: yymmdd_xxxxx
     */
    public static String generateAppTransId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        long random = System.currentTimeMillis() % 1000000;
        return date + "_" + random;
    }

    /**
     * Generate MAC (HMAC SHA256)
     * Format: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
     */
    public static String generateMac(
            int appId,
            String appTransId,
            String appUser,
            long amount,
            long appTime,
            String embedData,
            String item,
            String key1
    ) {
        String data = appId + "|" + appTransId + "|" + appUser + "|" +
                      amount + "|" + appTime + "|" + embedData + "|" + item;

        log.debug("MAC data: {}", data);

        return HmacUtils.hmacSha256Hex(key1, data);
    }

    /**
     * Verify callback MAC
     * Format: data|key2
     */
    public static boolean verifyCallbackMac(String data, String mac, String key2) {
        String calculatedMac = HmacUtils.hmacSha256Hex(key2, data);
        return calculatedMac.equals(mac);
    }

    /**
     * Convert embed_data map to JSON string
     */
    public static String toEmbedData(Map<String, Object> embedDataMap) {
        try {
            return objectMapper.writeValueAsString(embedDataMap);
        } catch (Exception e) {
            log.error("Error converting embed data to JSON", e);
            return "{}";
        }
    }

    /**
     * Convert item array to JSON string
     * Format: [{"itemid":"...", "itemname":"...", "itemprice":..., "itemquantity":...}]
     */
    public static String toItemJson(String itemName, long price) {
        String item = String.format(
            "[{\"itemid\":\"1\",\"itemname\":\"%s\",\"itemprice\":%d,\"itemquantity\":1}]",
            itemName, price
        );
        return item;
    }
}