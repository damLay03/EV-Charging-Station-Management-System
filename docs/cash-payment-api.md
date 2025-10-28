# API Thanh Toán Bằng Tiền Mặt (Cash Payment)

## Tổng quan
API này cho phép driver yêu cầu thanh toán bằng tiền mặt sau khi hoàn thành phiên sạc, và staff có thể xem danh sách yêu cầu cũng như xác nhận đã nhận tiền.

**Thiết kế tối ưu**: Sử dụng luôn bảng `payments` có sẵn với field `paymentMethod` ("CASH", "VNPAY") thay vì tạo bảng riêng, giúp tiết kiệm t��i nguyên và đơn giản hóa database schema.

## Flow hoạt đ���ng

### Flow cho Driver:
1. Driver kết thúc phiên sạc (status = COMPLETED)
2. Driver vào trang lịch sử xem các session đã hoàn thành
3. Driver bấm nút "Thanh toán ngay" → gọi API request cash payment
4. Hệ thống tạo/update payment record với:
   - `paymentMethod` = "CASH"
   - `status` = PENDING_CASH
   - `assignedStaff` = staff quản lý trạm
5. Yêu cầu hiển thị trong danh sách của staff

### Flow cho Staff:
1. Staff vào trang quản lý thanh toán tiền mặt
2. Staff xem danh sách payments với status = PENDING_CASH tại trạm của mình
3. Khi driver đến thanh toán, staff bấm "Xác nhận đã thanh toán"
4. Hệ thống cập nhật:
   - `status` = COMPLETED
   - `confirmedByStaff` = staff hiện tại
   - `confirmedAt` = thời gian hiện tại

---

## 1. Driver - Yêu cầu thanh toán bằng tiền mặt

### Endpoint
```
POST /api/cash-payments/request/{sessionId}
```

### Mô tả
Driver gửi yêu cầu thanh toán bằng tiền mặt cho một session đã hoàn thành.

### Authorization
- **Role**: DRIVER
- **Header**: `Authorization: Bearer {JWT_TOKEN}`

### Path Parameters
| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| sessionId | string | Có | ID của phiên sạc đã hoàn thành |

### Request Example
```http
POST /api/cash-payments/request/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response Success (200 OK)
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "requestId": "pay-789012",
    "paymentId": "pay-789012",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "driverId": "driver-001",
    "driverName": "Nguyễn Văn A",
    "driverPhone": "0901234567",
    "stationName": "Vincom Đồng Khởi",
    "chargingPointName": "TS1",
    "sessionStartTime": "2025-10-28T10:30:00",
    "sessionEndTime": "2025-10-28T11:45:00",
    "energyKwh": 32.5,
    "amount": 97500.0,
    "status": "PENDING",
    "createdAt": "2025-10-28T11:46:00",
    "confirmedAt": null,
    "confirmedByStaffName": null,
    "vehicleModel": "VinFast VF8",
    "licensePlate": "30A-12345"
  }
}
```

### Error Responses

#### Session không tồn tại (404)
```json
{
  "code": 9001,
  "message": "Charging Session Not Found"
}
```

#### Session chưa hoàn thành (400)
```json
{
  "code": 18001,
  "message": "Charging Session Not Completed"
}
```

#### Đã thanh toán rồi (400)
```json
{
  "code": 16002,
  "message": "Payment Already Completed"
}
```

#### Đã tồn tại yêu cầu thanh toán tiền mặt (400)
```json
{
  "code": 18002,
  "message": "Cash Payment Request Already Exists"
}
```

#### Trạm không có staff (500)
```json
{
  "code": 18003,
  "message": "Station Has No Assigned Staff"
}
```

---

## 2. Staff - Lấy danh sách yêu cầu thanh toán đang chờ

### Endpoint
```
GET /api/cash-payments/staff/pending
```

### Mô tả
Staff lấy danh sách các payment với status = PENDING_CASH tại trạm mà staff quản lý.

### Authorization
- **Role**: STAFF
- **Header**: `Authorization: Bearer {JWT_TOKEN}`

### Request Example
```http
GET /api/cash-payments/staff/pending
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response Success (200 OK)
```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "requestId": "pay-789012",
      "paymentId": "pay-789012",
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "driverId": "driver-001",
      "driverName": "Nguyễn Văn A",
      "driverPhone": "0901234567",
      "stationName": "Vincom Đồng Khởi",
      "chargingPointName": "TS1",
      "sessionStartTime": "2025-10-28T10:30:00",
      "sessionEndTime": "2025-10-28T11:45:00",
      "energyKwh": 32.5,
      "amount": 97500.0,
      "status": "PENDING",
      "createdAt": "2025-10-28T11:46:00",
      "confirmedAt": null,
      "confirmedByStaffName": null,
      "vehicleModel": "VinFast VF8",
      "licensePlate": "30A-12345"
    }
  ]
}
```

---

## 3. Staff - Xác nhận đã nhận tiền mặt

### Endpoint
```
PUT /api/cash-payments/staff/confirm/{paymentId}
```

### Mô tả
Staff xác nhận driver đã thanh toán tiền mặt. Sau khi xác nhận, payment status sẽ chuyển thành COMPLETED.

**⚠️ Quan trọng**: Staff chỉ được phép xác nhận các payment request tại tr���m mà họ quản lý. Nếu cố gắng xác nhận payment từ trạm khác sẽ bị từ chối với lỗi 18007.

### Authorization
- **Role**: STAFF
- **Header**: `Authorization: Bearer {JWT_TOKEN}`
- **Ràng buộc**: Staff chỉ có thể xác nhận payment tại station mà họ quản lý

### Path Parameters
| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| paymentId | string | Có | ID của payment (lấy từ response của GET pending) |

### Request Example
```http
PUT /api/cash-payments/staff/confirm/pay-789012
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response Success (200 OK)
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "requestId": "pay-789012",
    "paymentId": "pay-789012",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "driverId": "driver-001",
    "driverName": "Nguyễn Văn A",
    "driverPhone": "0901234567",
    "stationName": "Vincom Đồng Khởi",
    "chargingPointName": "TS1",
    "sessionStartTime": "2025-10-28T10:30:00",
    "sessionEndTime": "2025-10-28T11:45:00",
    "energyKwh": 32.5,
    "amount": 97500.0,
    "status": "CONFIRMED",
    "createdAt": "2025-10-28T11:46:00",
    "confirmedAt": "2025-10-28T12:00:00",
    "confirmedByStaffName": "Phạm Văn C",
    "vehicleModel": "VinFast VF8",
    "licensePlate": "30A-12345"
  }
}
```

### Error Responses

#### Payment không tồn tại (404)
```json
{
  "code": 8001,
  "message": "Payment Not Found"
}
```

#### Staff không quản lý station nào (400)
```json
{
  "code": 18004,
  "message": "Staff Does Not Manage Any Station"
}
```

#### Staff không có quyền xử lý payment này (403)
```json
{
  "code": 18007,
  "message": "Staff Not Authorized To Process Payments For This Station"
}
```
**Mô tả**: Lỗi này xảy ra khi staff cố gắng xác nhận payment từ một station khác (không phải station mà họ quản lý).

#### Payment request đã được xử lý (400)
```json
{
  "code": 18006,
  "message": "Cash Payment Request Already Processed"
}
```

---

## Database Schema

### Bảng `payments` - Thêm 3 cột mới

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| assigned_staff_id | VARCHAR(36) | Staff được assign xử lý thanh toán tiền mặt |
| confirmed_by_staff_id | VARCHAR(36) | Staff đã xác nhận thanh toán tiền mặt |
| confirmed_at | TIMESTAMP | Thời gian staff xác nhận thanh toán tiền mặt |

**Ưu đi��m so với tạo bảng mới:**
- ✅ Tiết kiệm tài nguyên database
- ✅ Không cần JOIN thêm bảng khi query
- ✅ Sử dụng lại các field có sẵn: `paymentMethod`, `status`, `amount`
- ✅ Đơn giản hóa logic business
- ✅ Dễ maintain và scale

---

## Payment Status Flow

```
PENDING_CASH (Driver yêu cầu thanh toán)
    ↓
    Staff xác nhận
    ↓
COMPLETED (Hoàn thành)
```

**Các status liên quan:**
- `PENDING_CASH`: Driver đã yêu cầu thanh toán tiền mặt, chờ staff xác nhận
- `COMPLETED`: Staff đã xác nhận nhận được tiền
- `CANCELLED`: Đã hủy (nếu cần)

---

## Testing

### Test Case 1: Driver yêu cầu thanh toán thành công
```bash
POST /api/cash-payments/request/{sessionId}
# Expected: Payment created v��i status = PENDING_CASH
```

### Test Case 2: Staff xem danh sách pending
```bash
GET /api/cash-payments/staff/pending
# Expected: Danh sách payments với status = PENDING_CASH tại trạm của staff
```

### Test Case 3: Staff xác nhận thanh toán
```bash
PUT /api/cash-payments/staff/confirm/{paymentId}
# Expected: Payment status = COMPLETED, confirmedAt được set
```
