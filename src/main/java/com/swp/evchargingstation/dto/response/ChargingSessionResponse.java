package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.ChargingSessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingSessionResponse {
    String sessionId;

    // Thông tin thời gian
    LocalDateTime startTime;        // Ngày và giờ bắt đầu
    LocalDateTime endTime;          // Ngày và giờ kết thúc
    Integer durationMin;            // Thời gian (45 phút, 1h 20m)

    // Thông tin trạm
    String stationName;             // Tên trạm (Vincom Đồng Khởi, Landmark 81)
    String stationAddress;          // Địa chỉ trạm
    String chargingPointName;       // Tên điểm sạc

    // Thông tin năng lượng và pin
    Integer startSocPercent;        // % pin lúc bắt đầu
    Integer endSocPercent;          // % pin lúc kết thúc
    Float energyKwh;                // Năng lượng tiêu thụ (32.5 kWh)

    // Thông tin chi phí
    Float costTotal;                // Chi phí (113,750đ)

    // Trạng thái
    ChargingSessionStatus status;   // COMPLETED, IN_PROGRESS, CANCELLED

    // Thông tin xe
    String vehicleModel;            // Model xe
    String licensePlate;            // Biển số xe
}
package com.swp.evchargingstation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverDashboardResponse {
    // Thông tin tổng quan
    Double totalCost;           // Tổng chi phí (727,690đ)
    Double totalEnergyKwh;      // Tổng năng lượng (212.9 kWh)
    Integer totalSessions;       // Số phiên sạc (5)
    String averageCostPerMonth;  // TB/tháng (3418đ)

    // Thông tin xe và pin
    String vehicleModel;         // Tesla Model 3
    String licensePlate;         // Biển số: 30A-12345
    Integer currentBatterySoc;   // % pin hiện tại: 75%
}

