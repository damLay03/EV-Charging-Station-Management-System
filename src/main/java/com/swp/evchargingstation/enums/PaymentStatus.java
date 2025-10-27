package com.swp.evchargingstation.enums;

public enum PaymentStatus {
    PENDING,           // Đang chờ xử lý (VNPay)
    PENDING_CASH,      // Đang chờ staff xác nhận thanh toán tiền mặt
    COMPLETED,         // Đã hoàn thành
    FAILED,            // Thất bại
    REFUNDED,          // Đã hoàn tiền
    CANCELLED          // Đã hủy
}
