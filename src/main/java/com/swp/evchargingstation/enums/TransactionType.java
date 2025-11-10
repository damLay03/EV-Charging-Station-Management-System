package com.swp.evchargingstation.enums;

public enum TransactionType {
    TOPUP_ZALOPAY,           // Nạp tiền qua ZaloPay (13 chars)
    TOPUP_CASH,              // Nạp tiền mặt qua Staff (10 chars)
    BOOKING_DEPOSIT,         // Trừ tiền đặt cọc (15 chars)
    BOOKING_DEPOSIT_REFUND,  // Hoàn lại tiền đặt cọc (22 chars)
    CHARGING_PAYMENT,        // Thanh toán hóa đơn sạc (16 chars)
    PLAN_SUBCRIPTION,        // Đăng ký/gia hạn gói cước
    ADMIN_ADJUSTMENT         // Giao dịch điều chỉnh bởi Admin (16 chars)
}


