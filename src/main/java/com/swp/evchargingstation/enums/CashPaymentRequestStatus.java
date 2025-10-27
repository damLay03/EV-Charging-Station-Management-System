package com.swp.evchargingstation.enums;

public enum CashPaymentRequestStatus {
    PENDING,      // Đang chờ staff xác nhận (map từ PaymentStatus.PENDING_CASH)
    CONFIRMED,    // Staff đã xác nhận thanh toán (map từ PaymentStatus.COMPLETED)
    CANCELLED     // Đã hủy (map từ PaymentStatus.CANCELLED)
}

