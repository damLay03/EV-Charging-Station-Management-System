package com.swp.evchargingstation.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    /**
     * Legacy API used in VNPayService. When encodeKey = true, build query (encode keys and values).
     * When encodeKey = false, build hash data (encode values only). Keys sorted A-Z in both cases.
     */
    public static String getPaymentURL(Map<String, String> paramsMap, boolean encodeKey) {
        return encodeKey ? buildQuery(paramsMap) : buildHashData(paramsMap);
    }

    /**
     * Build query string for VNPay URL: encode both key and value, sort A-Z
     * Encoding must be US-ASCII like VNPay sample.
     */
    public static String buildQuery(Map<String, String> paramsMap) {
        List<String> fieldNames = new ArrayList<>(paramsMap.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = paramsMap.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (query.length() > 0) query.append('&');
                try {
                    query.append(URLEncoder.encode(fieldName, "US-ASCII"));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    query.append(fieldName).append('=').append(fieldValue);
                }
            }
        }
        return query.toString();
    }

    /**
     * Build hash data string for VNPay HMAC: sort A-Z, encode ONLY values with US-ASCII.
     */
    public static String buildHashData(Map<String, String> paramsMap) {
        List<String> fieldNames = new ArrayList<>(paramsMap.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = paramsMap.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (hashData.length() > 0) hashData.append('&');
                hashData.append(fieldName).append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    hashData.append(fieldValue);
                }
            }
        }
        return hashData.toString();
    }
}
