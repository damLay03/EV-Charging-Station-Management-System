package com.swp.evchargingstation.enums;

public enum ChargingPointStatus {
    AVAILABLE,
    OCCUPIED,        // Đang có người sử dụng (sau khi check-in)
    CHARGING,        // Đang sạc
    OUT_OF_SERVICE,
    MAINTENANCE,
    RESERVED         // Đã được đặt trước
}
