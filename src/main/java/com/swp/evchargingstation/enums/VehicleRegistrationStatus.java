package com.swp.evchargingstation.enums;

public enum VehicleRegistrationStatus {
    PENDING,     // Đang chờ admin xét duyệt
    APPROVED,    // Đã được phê duyệt - xe có thể sử dụng
    REJECTED     // Bị từ chối - driver có thể nộp lại
}

