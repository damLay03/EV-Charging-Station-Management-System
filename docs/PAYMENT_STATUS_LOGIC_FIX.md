# Fix Payment Status Logic - Loại bỏ PENDING_CASH

## Vấn đề ban đầu
- API `/api/cash-payments/request/{sessionId}` bị lỗi 500: "Data truncated for column 'status' at row 1"
- Nguyên nhân: Code đang dùng `PaymentStatus.PENDING_CASH` (12 ký tự) nhưng cột database quá ngắn
- **Logic không đúng**: Dùng status riêng cho cash payment không hợp lý

## Phân tích logic đúng
**Flow thanh toán đúng:**
1. Session hoàn thành → Payment được tạo với `status = PENDING`
2. Driver chọn phương thức thanh toán:
   - Chọn VNPay → Chuyển sang VNPay flow
   - Chọn Cash → Tạo request cho staff (vẫn giữ `status = PENDING`)
3. Staff xác nhận nhận tiền → `status = COMPLETED`

**Cách phân biệt cash payment request:**
- `status = PENDING` (đang chờ xử lý)
- `paymentMethod = "CASH"` (phương thức thanh toán tiền mặt)
- `assignedStaff IS NOT NULL` (đã assign cho staff)

## Các thay đổi đã thực hiện

### 1. PaymentStatus Enum
**Trước:**
```java
public enum PaymentStatus {
    PENDING,
    PENDING_CASH,  // ← XÓA
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
```

**Sau:**
```java
public enum PaymentStatus {
    PENDING,      // Đang chờ xử lý
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
```

### 2. CashPaymentService
**Thay đổi:**
- `requestCashPayment()`: Set `status = PENDING` thay vì `PENDING_CASH`
- `getPendingCashPaymentRequests()`: Query theo `status = PENDING + paymentMethod = CASH + assignedStaff != NULL`
- `confirmCashPayment()`: Check điều kiện mới
- `mapPaymentStatusToCashStatus()`: Map `PENDING` thay vì `PENDING_CASH`

### 3. PaymentRepository
**Thêm method mới:**
```java
@Query("SELECT p FROM Payment p " +
       "JOIN p.chargingSession cs " +
       "JOIN cs.chargingPoint cp " +
       "WHERE cp.station.stationId = :stationId " +
       "AND p.status = 'PENDING' " +
       "AND p.paymentMethod = 'CASH' " +
       "AND p.assignedStaff IS NOT NULL " +
       "ORDER BY p.createdAt DESC")
List<Payment> findPendingCashPaymentsByStationId(@Param("stationId") String stationId);
```

### 4. StaffDashboardService
**Thay đổi:**
- `getPendingCashPayments()`: Dùng `findPendingCashPaymentsByStationId()` thay vì `findByStationIdAndStatus()`

### 5. PaymentController
**Thay đổi:**
- Response của `/api/payment/cash/request`: Trả về `"PENDING"` thay vì `"PENDING_CASH"`

### 6. CashPaymentRequestStatus Enum
**Cập nhật comment:**
```java
public enum CashPaymentRequestStatus {
    PENDING,      // Map từ PaymentStatus.PENDING (không phải PENDING_CASH)
    CONFIRMED,
    CANCELLED
}
```

## Database Migration

**File:** `docs/fix-payment-status-column.sql`

```sql
-- Nếu database có data PENDING_CASH cũ, chạy query này:
UPDATE payments 
SET status = 'PENDING' 
WHERE status = 'PENDING_CASH';
```

**Lưu ý:** KHÔNG CẦN thay đổi schema vì cột `status` đã hỗ trợ PENDING

## Kết quả

✅ **Logic đúng hơn:** Payment status chỉ có PENDING, không cần PENDING_CASH riêng
✅ **Không bị lỗi truncate:** PENDING chỉ 7 ký tự, ngắn hơn nhiều
✅ **Dễ hiểu hơn:** Phân biệt payment type bằng `paymentMethod` field
✅ **Mở rộng dễ dàng:** Thêm payment method mới không cần thêm status

## Testing

**Các API cần test:**
1. `POST /api/cash-payments/request/{sessionId}` - Driver tạo cash payment request
2. `GET /api/cash-payments/staff/pending` - Staff xem pending requests
3. `PUT /api/cash-payments/staff/confirm/{paymentId}` - Staff xác nhận payment

**Expected behavior:**
- Payment được tạo với `status = PENDING`, `paymentMethod = CASH`
- Staff query được pending cash payments
- Sau khi confirm, `status = COMPLETED`

## Rollback

Nếu cần rollback về logic cũ (không khuyến khích):
1. Restore `PENDING_CASH` vào enum
2. Chạy: `ALTER TABLE payments MODIFY COLUMN status VARCHAR(20)`
3. Revert các thay đổi trong code

