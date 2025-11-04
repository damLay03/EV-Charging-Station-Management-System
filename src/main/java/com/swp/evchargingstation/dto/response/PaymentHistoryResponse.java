package com.swp.evchargingstation.dto.response;

import com.swp.evchargingstation.entity.Payment;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentHistoryResponse {
    String paymentId;
    LocalDateTime paymentTime;      // Thời gian thanh toán (15:45)
    String chargingPointName;       // Điểm sạc (Điểm sạc #2)
    String customerName;            // Khách hàng (Lê Văn Cường)
    String durationFormatted;       // Thời gian sạc (35 phút, 1h 15m)
    Float durationMinutes;          // Thời gian sạc (phút) - để sort nếu cần
    Float amount;                   // Số tiền (65,000đ)
    Payment.PaymentMethod paymentMethod;  // PT Thanh toán (CASH, ZALOPAY)
    String paymentMethodDisplay;    // Hiển thị (Thẻ, Tiền mặt, MoMo)
    String sessionId;               // ID session để link nếu cần
}

