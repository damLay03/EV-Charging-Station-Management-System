# API Tra cứu Thông tin Xe theo Biển số (Staff)

## Mô tả
API này cho phép staff nhập biển số xe và lấy được mọi thông tin cần thiết để khởi động phiên sạc.

---

## Endpoint: Tra cứu xe theo biển số

### GET `/api/staff/vehicles/lookup`

Tra cứu thông tin chi tiết của xe theo biển số, bao gồm thông tin xe, chủ xe, và trạng thái phiên sạc hiện tại.

**Quyền truy cập:** `STAFF` (Bearer Token)

**Query Parameters:**

| Tên | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| licensePlate | String | Có | Biển số xe cần tra cứu |

---

### Request Example

```http
GET /api/staff/vehicles/lookup?licensePlate=51A-12345
Authorization: Bearer {staff_token}
```

---

### Response Success (200 OK)

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    // Thông tin xe
    "vehicleId": "550e8400-e29b-41d4-a716-446655440000",
    "licensePlate": "51A-12345",
    "model": "MODEL_3_STANDARD_RANGE",
    "modelName": "Model 3 Standard Range",
    "brand": "TESLA",
    "brandDisplayName": "Tesla",
    "currentSocPercent": 45,
    "batteryCapacityKwh": 50.0,
    "batteryType": "LFP",
    "maxChargingPower": "170 kW DC",
    "maxChargingPowerKw": 170.0,
    
    // Thông tin chủ xe (driver)
    "ownerId": "driver-uuid-123",
    "ownerName": "Nguyễn Văn A",
    "ownerEmail": "nguyenvana@example.com",
    "ownerPhone": "0912345678",
    
    // Thông tin phiên sạc
    "hasActiveSession": false,
    "activeSessionId": null
  }
}
```

### Response với phiên sạc đang hoạt động

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "vehicleId": "550e8400-e29b-41d4-a716-446655440000",
    "licensePlate": "51A-12345",
    "model": "MODEL_3_STANDARD_RANGE",
    "modelName": "Model 3 Standard Range",
    "brand": "TESLA",
    "brandDisplayName": "Tesla",
    "currentSocPercent": 45,
    "batteryCapacityKwh": 50.0,
    "batteryType": "LFP",
    "maxChargingPower": "170 kW DC",
    "maxChargingPowerKw": 170.0,
    "ownerId": "driver-uuid-123",
    "ownerName": "Nguyễn Văn A",
    "ownerEmail": "nguyenvana@example.com",
    "ownerPhone": "0912345678",
    "hasActiveSession": true,
    "activeSessionId": "session-uuid-456"
  }
}
```

---

### Response Error Cases

#### 1. Xe không tồn tại (404)

```json
{
  "code": 5001,
  "message": "Vehicle Not Found"
}
```

#### 2. Driver không tồn tại (404)

```json
{
  "code": 6001,
  "message": "Driver Not Found"
}
```

#### 3. Chưa đăng nhập hoặc không có quyền (401/403)

```json
{
  "code": 1005,
  "message": "Unauthenticated"
}
```

---

## Luồng sử dụng

### Bước 1: Staff nhập biển số xe
Staff nhập biển số xe của khách hàng vào hệ thống.

### Bước 2: Hệ thống tra cứu thông tin
API sẽ trả về:
- **Thông tin xe**: Model, hãng, dung lượng pin, SOC hiện tại, công suất sạc tối đa
- **Thông tin chủ xe**: Tên, email, số điện thoại
- **Trạng thái phiên sạc**: Xe đang sạc hay không

### Bước 3: Staff kiểm tra và khởi động phiên sạc
- Nếu `hasActiveSession = false`: Staff có thể khởi động phiên sạc mới
- Nếu `hasActiveSession = true`: Xe đang trong phiên sạc, staff cần xử lý phiên sạc hiện tại trước

### Bước 4: Khởi động phiên sạc (nếu cần)
Staff sử dụng `vehicleId` và thông tin charging point để gọi API khởi động phiên sạc:

```http
POST /api/charging-sessions/start
```

```json
{
  "vehicleId": "550e8400-e29b-41d4-a716-446655440000",
  "chargingPointId": "point-uuid-789",
  "targetSocPercent": 80
}
```

---

## Thông tin chi tiết các trường

### Vehicle Information

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| vehicleId | String | ID duy nhất của xe (dùng để start session) |
| licensePlate | String | Biển số xe |
| model | Enum | Model code của xe |
| modelName | String | Tên model hiển thị |
| brand | Enum | Hãng xe (TESLA, VINFAST, BYD, v.v.) |
| brandDisplayName | String | Tên hãng hiển thị |
| currentSocPercent | Integer | % pin hiện tại (0-100) |
| batteryCapacityKwh | Float | Dung lượng pin (kWh) |
| batteryType | String | Loại pin (LFP, NMC, v.v.) |
| maxChargingPower | String | Công suất sạc tối đa (dạng text) |
| maxChargingPowerKw | Float | Công suất sạc tối đa (kW) |

### Owner Information

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| ownerId | String | ID của driver/chủ xe |
| ownerName | String | Họ tên chủ xe |
| ownerEmail | String | Email chủ xe |
| ownerPhone | String | Số điện thoại chủ xe |

### Session Status

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| hasActiveSession | Boolean | Xe có đang sạc không |
| activeSessionId | String | ID phiên sạc (nếu đang sạc) |

---

## Use Cases

### 1. Khách hàng đến trạm sạc, staff hỗ trợ
- Staff hỏi biển số xe
- Tra cứu thông tin qua API
- Hiển thị thông tin xe và chủ xe để xác nhận
- Chọn charging point và khởi động phiên sạc

### 2. Kiểm tra xe đang sạc
- Staff nhập biển số
- Nếu `hasActiveSession = true`, xem chi tiết phiên sạc qua `activeSessionId`
- Có thể dừng hoặc giám sát phiên sạc

### 3. Khách hàng quên thông tin tài khoản
- Staff có thể tra cứu bằng biển số xe
- Lấy thông tin liên hệ của chủ xe để hỗ trợ

---

## Notes

- API chỉ dành cho staff, yêu cầu role `STAFF`
- Biển số xe phải khớp chính xác (case-sensitive)
- Thông tin SOC (State of Charge) là % pin hiện tại của xe
- Staff cần kiểm tra `hasActiveSession` trước khi khởi động phiên sạc mới
- Mọi thông tin cá nhân của driver chỉ hiển thị cho staff có quyền

---

## Related APIs

- **POST** `/api/charging-sessions/start` - Khởi động phiên sạc
- **GET** `/api/staff/charging-points` - Danh sách charging points của trạm
- **GET** `/api/charging-sessions/{sessionId}` - Chi tiết phiên sạc

---

## Error Handling

Staff app cần xử lý các trường hợp:
1. **Biển số không tồn tại**: Hiển thị thông báo "Xe chưa đăng ký trong hệ thống"
2. **Xe đang sạc**: Cảnh báo và hiển thị thông tin phiên sạc hiện tại
3. **Lỗi kết nối**: Thử lại hoặc báo lỗi kỹ thuật

---

## Security

- Chỉ staff đã đăng nhập mới được truy cập
- Bearer token phải hợp lệ và có role `STAFF`
- Không lưu cache thông tin cá nhân của driver
- Log mọi request tra cứu để audit

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-10-31 | Initial release - Vehicle lookup API for staff |

