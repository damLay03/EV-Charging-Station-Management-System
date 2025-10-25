# Staff Dashboard API Documentation

## Tổng quan

API Dashboard cho STAFF quản lý trạm sạc được gán, xem phiên sạc, xử lý thanh toán và báo cáo sự cố.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: STAFF

---

## Enums

### ChargingPointStatus
- `AVAILABLE`: Sẵn sàng sử dụng
- `IN_USE`: Đang được sử dụng
- `OFFLINE`: Ngưng hoạt động
- `MAINTENANCE`: Đang bảo trì

### ChargingSessionStatus
- `IN_PROGRESS`: Đang sạc
- `COMPLETED`: Hoàn thành
- `CANCELLED`: Đã hủy

### IncidentSeverity
- `LOW`: Mức độ thấp
- `MEDIUM`: Mức độ trung bình
- `HIGH`: Mức độ cao
- `CRITICAL`: Nghiêm trọng

---

## API Endpoints

### 1. Lấy tổng quan Dashboard

**Endpoint**: `GET /api/staff/dashboard`

**Mô tả**: Lấy thống kê tổng quan của trạm được gán cho staff (số phiên sạc hôm nay, doanh thu, trạng thái các điểm sạc).

**Quyền truy cập**: STAFF

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "todaySessionsCount": 15,
    "todayRevenue": 750000.0,
    "averageSessionDuration": 45.5,
    "stationId": "station-uuid-1",
    "stationName": "Trạm sạc Quận 1",
    "stationAddress": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "totalChargingPoints": 8,
    "availablePoints": 3,
    "chargingPoints": 4,
    "offlinePoints": 1
  }
}
```

**Response Fields**:
- `todaySessionsCount` (integer): Số phiên sạc hôm nay
- `todayRevenue` (number): Doanh thu hôm nay (VNĐ)
- `averageSessionDuration` (number, nullable): Thời gian trung bình mỗi phiên (phút)
- `stationId` (string): ID của trạm
- `stationName` (string): Tên trạm
- `stationAddress` (string): Địa chỉ trạm
- `totalChargingPoints` (integer): Tổng số điểm sạc
- `availablePoints` (integer): Số điểm đang AVAILABLE
- `chargingPoints` (integer): Số điểm đang IN_USE
- `offlinePoints` (integer): Số điểm OFFLINE hoặc MAINTENANCE

---

### 2. Lấy danh sách điểm sạc (với thông tin session hiện tại)

**Endpoint**: `GET /api/staff/charging-points`

**Mô tả**: Lấy tất cả điểm sạc của trạm với thông tin session đang chạy (nếu có).

**Quyền truy cập**: STAFF

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "pointId": "point-uuid-1",
      "name": "TS1",
      "maxPowerKw": 22.0,
      "status": "IN_USE",
      "currentSessionId": "session-uuid-123",
      "driverName": "Nguyễn Văn A",
      "vehicleModel": "VinFast VF8",
      "startTime": "2025-10-26T14:30:00",
      "currentSocPercent": 65
    },
    {
      "pointId": "point-uuid-2",
      "name": "TS2",
      "maxPowerKw": 22.0,
      "status": "AVAILABLE",
      "currentSessionId": null,
      "driverName": null,
      "vehicleModel": null,
      "startTime": null,
      "currentSocPercent": 0
    },
    {
      "pointId": "point-uuid-3",
      "name": "TS3",
      "maxPowerKw": 22.0,
      "status": "OFFLINE",
      "currentSessionId": null,
      "driverName": null,
      "vehicleModel": null,
      "startTime": null,
      "currentSocPercent": 0
    }
  ]
}
```

**Response Fields** (mỗi charging point):
- `pointId` (string): ID của điểm sạc
- `name` (string): Tên điểm sạc (TS1, TS2, TS3...)
- `maxPowerKw` (number): Công suất tối đa (kW)
- `status` (string): Trạng thái (AVAILABLE | IN_USE | OFFLINE | MAINTENANCE)
- `currentSessionId` (string, nullable): ID session đang chạy
- `driverName` (string, nullable): Tên driver đang sạc
- `vehicleModel` (string, nullable): Model xe đang sạc
- `startTime` (string, nullable): Thời gian bắt đầu sạc (ISO datetime)
- `currentSocPercent` (integer): % pin hiện tại của xe đang sạc

---

### 3. Lấy danh sách điểm sạc cơ bản

**Endpoint**: `GET /api/staff/my-station/charging-points`

**Mô tả**: Lấy danh sách điểm sạc cơ bản (không có thông tin session) của trạm được gán.

**Quyền truy cập**: STAFF

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "pointId": "point-uuid-1",
      "name": "TS1",
      "powerKw": 22.0,
      "status": "IN_USE",
      "stationId": "station-uuid-1"
    },
    {
      "pointId": "point-uuid-2",
      "name": "TS2",
      "powerKw": 22.0,
      "status": "AVAILABLE",
      "stationId": "station-uuid-1"
    }
  ]
}
```

**Lưu ý**: Nếu staff chưa được gán trạm nào, trả về mảng rỗng `[]`.

---

### 4. Lấy danh sách giao dịch (Transactions)

**Endpoint**: `GET /api/staff/transactions`

**Mô tả**: Lấy danh sách các phiên sạc (đã hoàn thành hoặc đang chạy) để xử lý thanh toán.

**Quyền truy cập**: STAFF

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "sessionId": "session-uuid-1",
      "driverName": "Nguyễn Văn A",
      "driverPhone": "0901234567",
      "vehicleModel": "VinFast VF8",
      "chargingPointId": "point-uuid-1",
      "startTime": "2025-10-26T14:30:00",
      "endTime": "2025-10-26T15:45:00",
      "energyKwh": 25.5,
      "durationMin": 75,
      "costTotal": 127500.0,
      "status": "COMPLETED",
      "isPaid": false
    },
    {
      "sessionId": "session-uuid-2",
      "driverName": "Trần Thị B",
      "driverPhone": "0987654321",
      "vehicleModel": "Tesla Model 3",
      "chargingPointId": "point-uuid-2",
      "startTime": "2025-10-26T10:15:00",
      "endTime": "2025-10-26T11:30:00",
      "energyKwh": 18.0,
      "durationMin": 60,
      "costTotal": 90000.0,
      "status": "COMPLETED",
      "isPaid": true
    }
  ]
}
```

**Response Fields** (mỗi transaction):
- `sessionId` (string): ID của phiên sạc
- `driverName` (string): Tên driver
- `driverPhone` (string): Số điện thoại driver
- `vehicleModel` (string): Model xe
- `chargingPointId` (string): ID điểm sạc đã sử dụng
- `startTime` (string): Thời gian bắt đầu
- `endTime` (string, nullable): Thời gian kết thúc
- `energyKwh` (number): Năng lượng đã sạc (kWh)
- `durationMin` (integer): Thời gian sạc (phút)
- `costTotal` (number): Tổng chi phí (VNĐ)
- `status` (string): Trạng thái (IN_PROGRESS | COMPLETED | CANCELLED)
- `isPaid` (boolean): Đã thanh toán chưa

---

### 5. Xử lý thanh toán cho driver

**Endpoint**: `POST /api/staff/process-payment`

**Mô tả**: Staff xử lý thanh toán tiền mặt hoặc thẻ cho driver sau khi hoàn thành sạc.

**Quyền truy cập**: STAFF

**Request Body**:
```json
{
  "sessionId": "session-uuid-1",
  "paymentMethodId": "pm-uuid-1",
  "amount": 127500.0
}
```

**Request Fields**:
- `sessionId` (string, required): ID của phiên sạc cần thanh toán
- `paymentMethodId` (string, required): ID phương thức thanh toán
- `amount` (number, required): Số tiền thanh toán (VNĐ)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": "Payment processed successfully"
}
```

**Error Response** (400 Bad Request):
```json
{
  "code": 1002,
  "message": "Session not found or already paid"
}
```

---

### 6. Lấy danh sách sự cố (Incidents)

**Endpoint**: `GET /api/staff/incidents`

**Mô tả**: Lấy danh sách các sự cố đã được báo cáo tại trạm.

**Quyền truy cập**: STAFF

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "incidentId": "incident-uuid-1",
      "reporterName": "Nguyễn Văn A",
      "stationName": "Trạm sạc Quận 1",
      "chargingPointId": "point-uuid-3",
      "reportedAt": "2025-10-26T09:30:00",
      "description": "Điểm sạc TS3 không hoạt động, không sạc được",
      "severity": "HIGH",
      "status": "OPEN",
      "assignedStaffName": "Nguyễn Văn B",
      "resolvedAt": null
    },
    {
      "incidentId": "incident-uuid-2",
      "reporterName": "Trần Thị C",
      "stationName": "Trạm sạc Quận 1",
      "chargingPointId": "point-uuid-1",
      "reportedAt": "2025-10-25T14:20:00",
      "description": "Màn hình hiển thị bị lỗi",
      "severity": "MEDIUM",
      "status": "RESOLVED",
      "assignedStaffName": "Nguyễn Văn B",
      "resolvedAt": "2025-10-26T08:00:00"
    }
  ]
}
```

**Response Fields** (mỗi incident):
- `incidentId` (string): ID của sự cố
- `reporterName` (string): Tên người báo cáo
- `stationName` (string): Tên trạm
- `chargingPointId` (string, nullable): ID điểm sạc gặp sự cố
- `reportedAt` (string): Thời gian báo cáo
- `description` (string): Mô tả sự cố
- `severity` (string): Mức độ nghiêm trọng (LOW | MEDIUM | HIGH | CRITICAL)
- `status` (string): Trạng thái (OPEN | IN_PROGRESS | RESOLVED)
- `assignedStaffName` (string, nullable): Staff được gán xử lý
- `resolvedAt` (string, nullable): Thời gian giải quyết

---

### 7. Tạo báo cáo sự cố

**Endpoint**: `POST /api/staff/incidents`

**Mô tả**: Staff báo cáo sự cố tại trạm của mình.

**Quyền truy cập**: STAFF

**Request Body**:
```json
{
  "stationId": "station-uuid-1",
  "chargingPointId": "point-uuid-3",
  "description": "Điểm sạc TS3 không hoạt động, cần kiểm tra",
  "severity": "HIGH"
}
```

**Request Fields**:
- `stationId` (string, required): ID của trạm
- `chargingPointId` (string, optional): ID điểm sạc gặp sự cố (nếu có)
- `description` (string, required): Mô tả chi tiết sự cố
- `severity` (string, required): Mức độ (LOW | MEDIUM | HIGH | CRITICAL)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "incidentId": "incident-uuid-new",
    "reporterName": "Staff Name",
    "stationName": "Trạm sạc Quận 1",
    "chargingPointId": "point-uuid-3",
    "reportedAt": "2025-10-26T16:00:00",
    "description": "Điểm sạc TS3 không hoạt động, cần kiểm tra",
    "severity": "HIGH",
    "status": "OPEN",
    "assignedStaffName": null,
    "resolvedAt": null
  }
}
```

---

### 8. Cập nhật sự cố

**Endpoint**: `PUT /api/staff/incidents/{incidentId}`

**Mô tả**: Staff cập nhật trạng thái hoặc thông tin sự cố.

**Quyền truy cập**: STAFF

**Path Parameters**:
- `incidentId` (string, required): ID của sự cố

**Request Body**:
```json
{
  "description": "Đã thay cáp sạc mới, điểm sạc hoạt động bình thường",
  "severity": "MEDIUM",
  "status": "RESOLVED"
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "incidentId": "incident-uuid-1",
    "reporterName": "Nguyễn Văn A",
    "stationName": "Trạm sạc Quận 1",
    "chargingPointId": "point-uuid-3",
    "reportedAt": "2025-10-26T09:30:00",
    "description": "Đã thay cáp sạc mới, điểm sạc hoạt động bình thường",
    "severity": "MEDIUM",
    "status": "RESOLVED",
    "assignedStaffName": "Nguyễn Văn B",
    "resolvedAt": "2025-10-26T16:30:00"
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Not found | Không tìm thấy tài nguyên |

---

## Lưu ý khi sử dụng

1. **Staff Assignment**: Staff phải được gán vào một trạm bởi ADMIN trước khi có thể truy cập các APIs này.

2. **Dashboard Data**: 
   - Dữ liệu dashboard cập nhật real-time
   - `todaySessionsCount` và `todayRevenue` được tính từ 00:00 đến 23:59 hôm nay

3. **Charging Points Status**:
   - **AVAILABLE**: Sẵn sàng, không có session nào
   - **IN_USE**: Đang có session đang chạy
   - **OFFLINE/MAINTENANCE**: Không thể sử dụng

4. **Payment Processing**:
   - Chỉ xử lý được payment cho sessions đã COMPLETED
   - Không thể thanh toán lại session đã paid

5. **Incidents**:
   - Staff có thể tạo và cập nhật incidents
   - Severity cao (HIGH, CRITICAL) cần xử lý ưu tiên
   - Status tự động cập nhật khi resolved

6. **Real-time Updates**:
   - Nên poll endpoint `/api/staff/charging-points` định kỳ (mỗi 5-10s) để cập nhật trạng thái
   - Session đang chạy sẽ có `currentSocPercent` tăng theo thời gian

