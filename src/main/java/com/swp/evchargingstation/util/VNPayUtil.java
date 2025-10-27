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
     * THEO ĐÚNG TÀI LIỆU VNPAY CHÍNH THỨC:
     * https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
     *
     * - Sắp xếp các tham số theo thứ tự alphabet (A-Z)
     * - encodeKey = true: cho query URL (encode cả key và value)
     * - encodeKey = false: cho hash data (KHÔNG encode gì cả, giữ nguyên)
     */
    public static String getPaymentURL(Map<String, String> paramsMap, boolean encodeKey) {
        // Tạo danh sách các field name và sắp xếp theo alphabet
        List<String> fieldNames = new ArrayList<>(paramsMap.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = paramsMap.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }

                if (encodeKey) {
                    // Cho query URL: encode cả key và value
                    try {
                        sb.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                        sb.append("=");
                        sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    } catch (UnsupportedEncodingException e) {
                        sb.append(fieldName).append("=").append(fieldValue);
                    }
                } else {
                    // Cho hash data: KHÔNG encode gì cả (theo tài liệu VNPay)
                    sb.append(fieldName);
                    sb.append("=");
                    sb.append(fieldValue);
                }
            }
        }

        return sb.toString();
    }
}
