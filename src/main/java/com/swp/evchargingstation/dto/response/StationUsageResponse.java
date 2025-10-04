package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationUsageResponse {
    String stationId;
    String stationName;
    String address;
    LocalDate date;

    // Tổng quan
    Integer totalChargingPoints;
    Integer currentInUsePoints; // Số điểm đang sạc hiện tại
    Integer currentAvailablePoints; // Số điểm available hiện tại
    Double currentUsagePercent; // % sử dụng hiện tại

    // Thống kê trong ngày
    Integer totalSessionsToday; // Tổng số session trong ngày
    Integer completedSessionsToday; // Số session đã hoàn thành
    Integer activeSessionsToday; // Số session đang diễn ra

    Double totalEnergyToday; // Tổng kWh đã sạc trong ngày
    Double totalRevenueToday; // Tổng doanh thu trong ngày

    // Phân tích theo giờ
    List<HourlyUsageData> hourlyUsage; // Mức độ sử dụng theo từng giờ
    Integer peakHour; // Giờ cao điểm (giờ có nhiều session nhất)
    Double peakUsagePercent; // % sử dụng cao nhất trong ngày
}