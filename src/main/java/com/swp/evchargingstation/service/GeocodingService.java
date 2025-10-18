package com.swp.evchargingstation.service;

import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Service để chuyển đổi địa chỉ thành tọa độ (Geocoding) sử dụng Nominatim API (OpenStreetMap)
 * API này miễn phí và không cần API key
 */
@Slf4j
@Service
public class GeocodingService {
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "EVChargingStationManagementSystem/1.0";

    private final RestTemplate restTemplate;

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Chuyển đổi địa chỉ thành tọa độ (latitude, longitude)
     * @param address Địa chỉ cần chuyển đổi
     * @return Map chứa "latitude" và "longitude"
     * @throws AppException nếu không tìm thấy địa chỉ hoặc có lỗi kết nối
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> geocodeAddress(String address) {
        try {
            log.info("Geocoding address: {}", address);

            // Xây dựng URL với các tham số
            URI uri = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", "1")
                    .queryParam("addressdetails", "1")
                    .build()
                    .toUri();

            // Gửi request với User-Agent header (bắt buộc theo chính sách của Nominatim)
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            // Gọi API
            var response = restTemplate.exchange(
                    uri,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    List.class
            );

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody();

            // Kiểm tra kết quả
            if (results == null || results.isEmpty()) {
                log.error("No geocoding results found for address: {}", address);
                // Sử dụng error code đã có trong hệ thống
                throw new AppException(ErrorCode.VALIDATION_FAILED);
            }

            // Lấy tọa độ từ kết quả đầu tiên
            Map<String, Object> firstResult = results.get(0);
            double latitude = Double.parseDouble(firstResult.get("lat").toString());
            double longitude = Double.parseDouble(firstResult.get("lon").toString());

            // Làm tròn đến 6 chữ số thập phân (chính xác ~11cm)
            latitude = Math.round(latitude * 1000000.0) / 1000000.0;
            longitude = Math.round(longitude * 1000000.0) / 1000000.0;

            log.info("Geocoded successfully: lat={}, lon={}", latitude, longitude);

            return Map.of(
                    "latitude", latitude,
                    "longitude", longitude
            );

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during geocoding: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }

    /**
     * Validate tọa độ có hợp lệ không
     * @param latitude Vĩ độ (-90 đến 90)
     * @param longitude Kinh độ (-180 đến 180)
     * @return true nếu hợp lệ
     */
    public boolean isValidCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }
}
