package com.swp.evchargingstation.enums;

public enum TransactionType {
    TOPUP_ZALOPAY,           // Nạp tiền qua ZaloPay
    TOPUP_CASH,              // Nạp tiền mặt qua Staff
    BOOKING_DEPOSIT,         // Trừ tiền đặt cọc
    BOOKING_DEPOSIT_REFUND,  // Hoàn lại tiền đặt cọc
    BOOKING_REFUND,          // Hoàn tiền khi hủy booking
    CHARGING_PAYMENT,        // Thanh toán hóa đơn sạc
    ADMIN_ADJUSTMENT         // Giao dịch điều chỉnh bởi Admin
}


