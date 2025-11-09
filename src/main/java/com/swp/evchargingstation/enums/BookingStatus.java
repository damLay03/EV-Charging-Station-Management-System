package com.swp.evchargingstation.enums;

public enum BookingStatus {
    CONFIRMED, // Đã cọc thành công, đang giữ trụ
    IN_PROGRESS, // Người dùng đã đến và đang sạc
    COMPLETED, // Đã sạc xong, hoàn tất
    CANCELLED_BY_USER, // Người dùng tự hủy
    EXPIRED // Quá giờ check-in, tự động hủy
}

