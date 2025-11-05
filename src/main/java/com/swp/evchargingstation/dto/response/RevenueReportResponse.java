package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueReportResponse {
    String reportPeriod;        // "Daily", "Weekly", "Monthly", "Custom Range"
    String periodDetails;       // "November 5, 2025", "Week 45, 2025", etc.
    LocalDateTime generatedAt;  // Thời gian tạo báo cáo

    // Tổng quan
    ReportSummary summary;

    // Chi tiết từng trạm
    List<StationRevenueResponse> stationDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReportSummary {
        float totalRevenue;          // Tổng doanh thu
        int totalSessions;           // Tổng số phiên sạc
        int totalStations;           // Tổng số trạm có doanh thu
        float averageRevenuePerStation; // Doanh thu trung bình mỗi trạm
        float averageRevenuePerSession; // Doanh thu trung bình mỗi phiên
        String topStation;           // Trạm có doanh thu cao nhất
        float topStationRevenue;     // Doanh thu của trạm cao nhất
    }
}

