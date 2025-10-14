package com.swp.evchargingstation.enums;

/**
 * Các loại thông báo trong hệ thống
 * - CHARGING_COMPLETE: Thông báo khi xe đã sạc đầy
 * - LOW_BATTERY: Cảnh báo khi pin dưới 20%
 * - PROMOTIONAL: Nhận thông báo về ưu đãi đặc biệt
 * - MAINTENANCE: Thông báo về lịch bảo trì trạm sạc
 */
public enum NotificationType {
    CHARGING_COMPLETE,  // Hoàn thành sạc
    LOW_BATTERY,        // Pin yếu
    PROMOTIONAL,        // Khuyến mãi
    MAINTENANCE         // Bảo trì trạm
}

