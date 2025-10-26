package com.swp.evchargingstation.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class VNPayUtil {

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            // FIX: Chuyển IPv6 localhost thành IPv4 - THEO HƯỚNG DẪN VNPAY
            if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
                ipAddress = "127.0.0.1";
            }
        } catch (Exception e) {
            ipAddress = "127.0.0.1"; // Default IPv4
        }
        return ipAddress;
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ĐÚNG THEO CODE MẪU VNPAY VÀ HƯỚNG DẪN CỘNG ĐỒNG
    // encodeKey = true: cho query URL (encode cả key và value)
    // encodeKey = false: cho hash data (KHÔNG encode key, KHÔNG encode value)
    public static String getPaymentURL(Map<String, String> paramsMap, boolean encodeKey) {
        return paramsMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if (encodeKey) {
                        // CHO QUERY URL: Encode và replace %20 thành +
                        try {
                            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString())
                                    .replace("%20", "+");
                            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                                    .replace("%20", "+");
                            return encodedKey + "=" + encodedValue;
                        } catch (UnsupportedEncodingException e) {
                            return key + "=" + value;
                        }
                    } else {
                        // CHO HASH DATA: KHÔNG ENCODE, giữ nguyên
                        return key + "=" + value;
                    }
                })
                .collect(Collectors.joining("&"));
    }
}
