# Cash Payment Authorization Logic Fix

## Vấn đề
Trước đây, staff có thể xử lý **tất cả** payment requests trong hệ thống, kể cả những payment không thuộc station mà họ quản lý. Đây là lỗi logic bảo mật nghiêm trọng.

## Giải pháp đã áp dụng

### 1. Thêm Error Code mới
**File**: `ErrorCode.java`

Thêm error code `STAFF_NOT_AUTHORIZED_FOR_STATION (18007)` để xử lý trường hợp staff cố gắng xử lý payment từ station khác.

```java
STAFF_NOT_AUTHORIZED_FOR_STATION(18007, "Staff Not Authorized To Process Payments For This Station")
```

### 2. Cải thiện logic kiểm tra trong CashPaymentService
**File**: `CashPaymentService.java`

#### Phương thức `confirmCashPayment()`:

**Trước khi fix:**
- Logic kiểm tra đã có nhưng chưa rõ ràng
- Error message không cụ thể
- Thiếu logging chi tiết

**Sau khi fix:**
```java
@Transactional
public CashPaymentRequestResponse confirmCashPayment(String paymentId) {
    String userId = getUserIdFromToken();
    
    // 1. Lấy thông tin staff
    Staff staff = staffRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    
    // 2. Kiểm tra staff có quản lý station nào không
    Station managedStation = staff.getManagedStation();
    if (managedStation == null) {
        log.error("Staff {} does not manage any station", userId);
        throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
    }
    
    // 3. Lấy payment
    Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    
    // 4. Kiểm tra payment có thuộc station của staff không
    Station paymentStation = payment.getChargingSession()
                                   .getChargingPoint()
                                   .getStation();
    
    if (!managedStation.getStationId().equals(paymentStation.getStationId())) {
        log.error("Staff {} (manages station {}) attempted to confirm payment {} from station {}",
                userId, managedStation.getStationId(), paymentId, paymentStation.getStationId());
        throw new AppException(ErrorCode.STAFF_NOT_AUTHORIZED_FOR_STATION);
    }
    
    // 5. Tiếp tục xử lý payment...
}
```

### 3. Cải thiện

#### ✅ Security Check Flow:
1. **Verify Staff Identity**: Lấy staff từ JWT token
2. **Verify Station Management**: Kiểm tra staff có quản lý station nào không
3. **Verify Payment Ownership**: Kiểm tra payment có thuộc station mà staff quản lý
4. **Detailed Logging**: Log đầy đủ thông tin khi có attempt unauthorized

#### ✅ Error Handling:
- Error code cụ thể cho từng trường hợp
- Message rõ ràng cho developer và user
- Logging chi tiết để audit và debug

## Kết quả

### Scenario 1: Staff hợp lệ xác nhận payment của station mình quản lý
```
✅ SUCCESS: Payment được xác nhận thành công
```

### Scenario 2: Staff cố gắng xác nhận payment từ station khác
```
❌ ERROR 18007: "Staff Not Authorized To Process Payments For This Station"
Log: "Staff staff-001 (manages station ST-001) attempted to confirm payment PAY-123 from station ST-002"
```

### Scenario 3: Staff không quản lý station nào
```
❌ ERROR 18004: "Staff Does Not Manage Any Station"
```

## API Documentation Update

Đã cập nhật file `cash-payment-api.md` với:
- ⚠️ Warning về authorization constraint
- Error response mới (18007)
- Mô tả chi tiết về security check

## Testing Recommendations

### Test Case 1: Happy Path
```bash
# Staff A quản lý Station 1
# Payment thuộc Station 1
PUT /api/cash-payments/staff/confirm/{paymentId}
Expected: 200 OK, payment confirmed
```

### Test Case 2: Unauthorized Access
```bash
# Staff A quản lý Station 1
# Payment thuộc Station 2
PUT /api/cash-payments/staff/confirm/{paymentId}
Expected: 403, Error 18007
```

### Test Case 3: Staff không quản lý station
```bash
# Staff không được assign làm manager của station nào
PUT /api/cash-payments/staff/confirm/{paymentId}
Expected: 400, Error 18004
```

## Impact Analysis

### ✅ Bảo mật tăng cường
- Staff chỉ có thể xử lý payment tại station của mình
- Ngăn chặn unauthorized access
- Audit trail đầy đủ qua logging

### ✅ Không ảnh hưởng breaking change
- API endpoint không thay đổi
- Response format không thay đổi
- Chỉ thêm validation logic

### ✅ User Experience
- Error message rõ ràng
- Staff biết chính xác lý do bị từ chối
- Developer dễ debug qua logs

## Files Changed

1. ✅ `ErrorCode.java` - Thêm error code 18007
2. ✅ `CashPaymentService.java` - Cải thiện authorization logic
3. ✅ `cash-payment-api.md` - Cập nhật documentation

## Deployment Notes

- **Database**: Không cần migration
- **Backward Compatibility**: Hoàn toàn tương thích
- **Testing**: Cần test các scenario authorization
- **Monitoring**: Theo dõi logs để phát hiện unauthorized attempts

---

**Ngày fix**: 2025-10-28  
**Developer**: GitHub Copilot  
**Priority**: HIGH (Security Issue)

