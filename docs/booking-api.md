# Booking System API Documentation

## Overview
Tính năng đặt chỗ trụ sạc (Booking System) cho phép người dùng đặt trước và đặt cọc một trụ sạc tại trạm cụ thể.

## API Endpoints

### 1. Check Availability
Kiểm tra khả năng đặt chỗ tại một trụ sạc cụ thể.

**Endpoint:** `GET /api/bookings/availability`

**Parameters:**
- `chargingPointId` (Long, required): ID của trụ sạc
- `bookingTime` (LocalDateTime, required): Thời gian muốn đặt (format: `yyyy-MM-ddTHH:mm:ss`)
- `vehicleId` (Long, required): ID của xe

**Example Request:**
```http
GET /api/bookings/availability?chargingPointId=1&bookingTime=2025-11-10T08:00:00&vehicleId=1
```

**Example Response:**
```json
{
  "available": true,
  "maxChargePercentage": 57.0,
  "message": "You can only charge up to 57.00% (session will end at 2025-11-10T09:00 for the next user)."
}
```

**Business Rules:**
- Chỉ có thể đặt trong vòng 24 giờ tới
- Không thể đặt trong quá khứ
- Hệ thống tính toán % pin tối đa dựa trên booking tiếp theo

---

### 2. Create Booking
Tạo đơn đặt chỗ mới.

**Endpoint:** `POST /api/bookings`

**Headers:**
- `Authorization: Bearer {token}` (required)

**Request Body:**
```json
{
  "vehicleId": 1,
  "chargingPointId": 1,
  "bookingTime": "2025-11-10T08:00:00",
  "desiredPercentage": 50.0
}
```

**Example Response:**
```json
{
  "id": 1,
  "user": {...},
  "vehicle": {...},
  "chargingPoint": {...},
  "bookingTime": "2025-11-10T08:00:00",
  "estimatedEndTime": "2025-11-10T08:45:00",
  "desiredPercentage": 50.0,
  "depositAmount": 50000.0,
  "bookingStatus": "CONFIRMED",
  "createdAt": "2025-11-09T10:30:00"
}
```

**Business Rules:**
- Yêu cầu authentication (phải đăng nhập)
- Kiểm tra số dư ví >= 50,000 VNĐ
- Trừ 50,000 VNĐ tiền cọc từ ví
- Đặt trạng thái trụ sạc thành `RESERVED`
- `desiredPercentage` phải <= `maxChargePercentage` từ availability check

**Error Responses:**

```json
// Insufficient funds
{
  "code": 19002,
  "message": "Insufficient Funds In Wallet"
}

// Invalid booking time
{
  "code": 1006,
  "message": "Validation Failed"
}

// Charging point not found
{
  "code": 11001,
  "message": "Charging Point Not Found"
}
```

---

## Booking Status Flow

1. **CONFIRMED** - Đã đặt cọc thành công, đang giữ trụ
2. **IN_PROGRESS** - Người dùng đã check-in và đang sạc
3. **COMPLETED** - Hoàn tất phiên sạc
4. **CANCELLED_BY_USER** - Người dùng hủy (chưa implement)
5. **EXPIRED** - Quá giờ check-in (mất cọc)

---

## Check-in Process

### Check-in Window
- **Cửa sổ check-in:** ±10 phút từ `bookingTime`
- Ví dụ: Đặt lúc 8:00 AM → có thể check-in từ 7:50 AM - 8:10 AM

### Check-in Success
1. Người dùng bắt đầu charging session trong cửa sổ check-in
2. Hệ thống kiểm tra:
   - Booking status = `CONFIRMED`
   - Vehicle ID khớp với booking
3. Update booking status → `IN_PROGRESS`
4. Update charging point status → `CHARGING`

### Check-in Failed (Expired)
- Nếu quá 8:10 AM mà chưa check-in
- Scheduled task (chạy mỗi phút) sẽ:
  - Set booking status → `EXPIRED`
  - Set charging point status → `AVAILABLE`
  - **Tiền cọc 50,000 VNĐ bị mất**

---

## Payment Processing

### Khi hoàn thành phiên sạc:

**Case 1: Hóa đơn > Tiền cọc**
- Tổng hóa đơn: 120,000 VNĐ
- Tiền cọc: 50,000 VNĐ
- → Trừ thêm: 70,000 VNĐ từ ví

**Case 2: Hóa đơn < Tiền cọc**
- Tổng hóa đơn: 30,000 VNĐ
- Tiền cọc: 50,000 VNĐ
- → Hoàn lại: 20,000 VNĐ vào ví

**Case 3: Hóa đơn = Tiền cọc**
- Không trừ thêm, không hoàn lại

---

## Testing Scenarios

### Scenario 1: Successful Booking
```bash
# 1. Check availability
GET /api/bookings/availability?chargingPointId=1&bookingTime=2025-11-10T08:00:00&vehicleId=1

# 2. Create booking
POST /api/bookings
{
  "vehicleId": 1,
  "chargingPointId": 1,
  "bookingTime": "2025-11-10T08:00:00",
  "desiredPercentage": 50.0
}

# 3. Check wallet balance (should be reduced by 50,000)
GET /api/wallet/balance

# 4. Start charging session (within check-in window)
POST /api/charging-sessions/start
```

### Scenario 2: Expired Booking
```bash
# 1. Create booking for 08:00
POST /api/bookings
{
  "bookingTime": "2025-11-10T08:00:00",
  ...
}

# 2. Wait until 08:11 (past check-in window)
# Scheduled task will auto-expire

# 3. Charging point should be AVAILABLE again
# Deposit is lost
```

### Scenario 3: Insufficient Balance
```bash
# 1. Try to book with balance < 50,000
POST /api/bookings
# Should return error: INSUFFICIENT_FUNDS
```

---

## Database Schema

### bookings table
```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    vehicle_id VARCHAR(255) NOT NULL,
    charging_point_id VARCHAR(255) NOT NULL,
    booking_time DATETIME NOT NULL,
    estimated_end_time DATETIME,
    desired_percentage FLOAT,
    deposit_amount DOUBLE NOT NULL,
    booking_status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (charging_point_id) REFERENCES charging_points(point_id)
);
```

---

## Scheduled Tasks

### Process Expired Bookings
- **Cron:** `0 * * * * *` (chạy mỗi phút)
- **Logic:**
  1. Tìm bookings có `status = CONFIRMED` và `bookingTime < (now - 10 minutes)`
  2. Update `status = EXPIRED`
  3. Update charging point `status = AVAILABLE`
  4. Tiền cọc bị mất (không hoàn lại)

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1006 | Validation Failed | Thời gian đặt không hợp lệ hoặc % pin vượt quá giới hạn |
| 11001 | Charging Point Not Found | Không tìm thấy trụ sạc |
| 19002 | Insufficient Funds | Số dư ví không đủ |
| 20001 | Vehicle Not Match Booking | Xe không khớp với booking khi check-in |
| 20002 | Charging Point Reserved | Trụ sạc đã được đặt trước |

---

## Notes for Frontend Development

1. **Datetime Format:** Sử dụng ISO 8601 format: `yyyy-MM-ddTHH:mm:ss`
2. **Check-in Timer:** Hiển thị countdown timer cho cửa sổ check-in (±10 phút)
3. **Wallet Balance:** Kiểm tra số dư trước khi cho phép đặt chỗ
4. **Availability Check:** Luôn gọi API này trước khi tạo booking
5. **Status Updates:** Poll API định kỳ để cập nhật trạng thái booking

---

## Implementation Status

✅ **Completed:**
- Booking entity & repository
- Check availability logic
- Create booking with deposit
- Check-in validation
- Expired booking scheduled task
- Payment processing (refund/debit)
- Error handling

⏳ **Future Enhancements:**
- Cancel booking by user
- Booking history API
- Push notifications for expired bookings
- Admin dashboard for booking management

