package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.enums.StationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StationDetailResponse {
    String stationId;
    String name; // Tên trạm
    String address; // Địa chỉ
    StationStatus status; // Trạng thái

    // Điểm sạc - thông tin tổng hợp
    Integer totalChargingPoints; // Tổng số điểm sạc
    Integer activeChargingPoints; // Số điểm đang hoạt động (AVAILABLE hoặc IN_USE)
    Integer offlineChargingPoints; // Số điểm offline
    Integer maintenanceChargingPoints; // Số điểm bảo trì
    String chargingPointsSummary; // "Tổng: X | Hoạt động: Y | Offline: Z | Bảo trì: W"

    Double revenue; // Doanh thu (tổng từ các session của trạm này)
    Double usagePercent; // Phần trăm sử dụng (số điểm đang IN_USE / tổng)
    String staffName; // Tên nhân viên quản lý (lấy từ staff được assign)
}

