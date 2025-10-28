package com.swp.evchargingstation.enums;

public enum PaymentStatus {
    UNPAID,            // Chưa thanh toán (tự động tạo khi session COMPLETED)
    PENDING,           // Đang chờ xử lý (đã chọn phương thức thanh toán)
    COMPLETED,         // Đã hoàn thành
    FAILED,            // Thất bại
    REFUNDED,          // Đã hoàn tiền
    CANCELLED          // Đã hủy
}
