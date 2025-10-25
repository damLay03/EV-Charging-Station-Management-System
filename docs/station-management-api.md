# Station Management API Documentation

## Tổng quan

API quản lý trạm sạc cho phép ADMIN tạo, cập nhật, xóa trạm sạc và các trụ sạc (charging points), cũng như quản lý trạng thái hoạt động của chúng.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: Hầu hết các endpoint yêu cầu role ADMIN

---

## Enums

### StationStatus
- `OPERATIONAL`: Đang hoạt động
- `OUT_OF_SERVICE`: Ngừng hoạt động
- `UNDER_MAINTENANCE`: Đang bảo trì

### ChargingPointStatus
- `AVAILABLE`: Sẵn sàng sử dụng
- `IN_USE`: Đang được sử dụng
- `OFFLINE`: Ngưng hoạt động
- `MAINTENANCE`: Đang bảo trì

### ChargingPower
- `SLOW_7KW`: Sạc chậm 7kW
- `FAST_22KW`: Sạc nhanh 22kW
- `RAPID_50KW`: Sạc siêu nhanh 50kW
- `ULTRA_RAPID_150KW`: Sạc cực nhanh 150kW

---

## API Endpoints

### STATION MANAGEMENT

#### 1. Lấy danh sách trạm sạc (Overview)

**Endpoint**: `GET /api/stations/overview`

**Mô tả**: Lấy danh sách tất cả trạm sạc với thông tin tổng quan.

**Quyền truy cập**: ADMIN

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "EV Charging Corp",
      "contactPhone": "0281234567",
      "latitude": 10.7769,
      "longitude": 106.7009,
      "status": "OPERATIONAL",
      "active": true,
      "staffId": "staff-uuid-1",
      "staffName": "Nguyễn Văn A"
    }
  ]
}
```

**Response Fields**:
- `stationId` (string): ID của trạm
- `name` (string): Tên trạm
- `address` (string): Địa chỉ
- `operatorName` (string, nullable): Tên đơn vị vận hành
- `contactPhone` (string, nullable): Số điện thoại
- `latitude` (number, nullable): Vĩ độ
- `longitude` (number, nullable): Kinh độ
- `status` (string): Trạng thái (OPERATIONAL | OUT_OF_SERVICE | UNDER_MAINTENANCE)
- `active` (boolean): true nếu status == OPERATIONAL
- `staffId` (string, nullable): ID nhân viên quản lý
- `staffName` (string, nullable): Tên nhân viên quản lý

---

#### 2. Lấy danh sách trạm sạc (Basic)

**Endpoint**: `GET /api/stations`

**Mô tả**: Lấy danh sách trạm sạc với thông tin cơ bản, có thể lọc theo status.

**Quyền truy cập**: Public (không cần authentication)

**Query Parameters**:
- `status` (string, optional): Lọc theo trạng thái (OPERATIONAL | OUT_OF_SERVICE | UNDER_MAINTENANCE)

**Example Request**:
```
GET /api/stations?status=OPERATIONAL
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "EV Charging Corp",
      "contactPhone": "0281234567",
      "latitude": 10.7769,
      "longitude": 106.7009,
      "status": "OPERATIONAL",
      "active": true,
      "staffId": "staff-uuid-1",
      "staffName": "Nguyễn Văn A"
    }
  ]
}
```

---

#### 3. Lấy danh sách trạm sạc chi tiết (Detail)

**Endpoint**: `GET /api/stations/detail`

**Mô tả**: Lấy danh sách trạm sạc với thông tin đầy đủ (bao gồm điểm sạc, doanh thu, % sử dụng).

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `status` (string, optional): Lọc theo trạng thái

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "latitude": 10.7769,
      "longitude": 106.7009,
      "status": "OPERATIONAL",
      "totalChargingPoints": 8,
      "activeChargingPoints": 5,
      "offlineChargingPoints": 1,
      "maintenanceChargingPoints": 2,
      "chargingPointsSummary": "Tổng: 8 | Hoạt động: 5 | Offline: 1 | Bảo trì: 2",
      "revenue": 25000000.0,
      "usagePercent": 62.5,
      "staffId": "staff-uuid-1",
      "staffName": "Nguyễn Văn A"
    }
  ]
}
```

**Response Fields**:
- `stationId` (string): ID của trạm
- `name` (string): Tên trạm
- `address` (string): Địa chỉ
- `latitude` (number, nullable): Vĩ độ
- `longitude` (number, nullable): Kinh độ
- `status` (string): Trạng thái
- `totalChargingPoints` (integer, nullable): Tổng số điểm sạc
- `activeChargingPoints` (integer, nullable): Số điểm đang hoạt động
- `offlineChargingPoints` (integer, nullable): Số điểm offline
- `maintenanceChargingPoints` (integer, nullable): Số điểm bảo trì
- `chargingPointsSummary` (string, nullable): Tóm tắt trạng thái điểm sạc
- `revenue` (number, nullable): Doanh thu
- `usagePercent` (number, nullable): % sử dụng
- `staffId` (string, nullable): ID nhân viên quản lý
- `staffName` (string, nullable): Tên nhân viên quản lý

---

#### 4. Tạo trạm sạc mới

**Endpoint**: `POST /api/stations/create`

**Mô tả**: ADMIN tạo trạm sạc mới trong hệ thống.

**Quyền truy cập**: ADMIN

**Request Body**:
```json
{
  "name": "Trạm sạc Quận 7",
  "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
  "numberOfChargingPoints": 4,
  "powerOutput": "FAST_22KW",
  "operatorName": "EV Charging Corp",
  "contactPhone": "0287654321",
  "latitude": 10.7329,
  "longitude": 106.7196,
  "staffId": "staff-uuid-1"
}
```

**Request Fields**:
- `name` (string, required): Tên trạm sạc
- `address` (string, required): Địa chỉ đầy đủ
- `numberOfChargingPoints` (integer, required): Số lượng điểm sạc (tối thiểu 1)
- `powerOutput` (string, required): Công suất sạc (SLOW_7KW | FAST_22KW | RAPID_50KW | ULTRA_RAPID_150KW)
- `operatorName` (string, optional): Tên đơn vị vận hành
- `contactPhone` (string, optional): Số điện thoại liên hệ
- `latitude` (number, optional): Vĩ độ (-90 đến 90) - nếu không gửi sẽ auto geocode
- `longitude` (number, optional): Kinh độ (-180 đến 180) - nếu không gửi sẽ auto geocode
- `staffId` (string, optional): ID của staff được gán quản lý

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-new",
    "name": "Trạm sạc Quận 7",
    "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0287654321",
    "latitude": 10.7329,
    "longitude": 106.7196,
    "status": "OPERATIONAL",
    "active": true,
    "staffId": "staff-uuid-1",
    "staffName": "Nguyễn Văn A"
  }
}
```

---

#### 5. Cập nhật thông tin trạm sạc

**Endpoint**: `PUT /api/stations/{stationId}`

**Mô tả**: ADMIN cập nhật thông tin trạm sạc.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Request Body**:
```json
{
  "name": "Trạm sạc Quận 7 - Updated",
  "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
  "operatorName": "EV Charging Corp",
  "contactPhone": "0287654321",
  "latitude": 10.7329,
  "longitude": 106.7196,
  "status": "UNDER_MAINTENANCE",
  "staffId": "staff-uuid-2"
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-1",
    "name": "Trạm sạc Quận 7 - Updated",
    "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0287654321",
    "latitude": 10.7329,
    "longitude": 106.7196,
    "status": "UNDER_MAINTENANCE",
    "active": false,
    "staffId": "staff-uuid-2",
    "staffName": "Trần Thị B"
  }
}
```

---

#### 6. Cập nhật trạng thái trạm sạc

**Endpoint**: `PATCH /api/stations/{stationId}/status`

**Mô tả**: Cập nhật trạng thái cụ thể của trạm sạc.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Query Parameters**:
- `status` (string, required): Trạng thái mới (OPERATIONAL | OUT_OF_SERVICE | UNDER_MAINTENANCE)

**Example Request**:
```
PATCH /api/stations/station-uuid-1/status?status=OUT_OF_SERVICE
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0281234567",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "status": "OUT_OF_SERVICE",
    "active": false,
    "staffId": "staff-uuid-1",
    "staffName": "Nguyễn Văn A"
  }
}
```

---

#### 7. Kích hoạt trạm sạc

**Endpoint**: `PATCH /api/stations/{stationId}/activate`

**Mô tả**: Đặt trạng thái trạm sạc thành OPERATIONAL.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0281234567",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "status": "OPERATIONAL",
    "active": true,
    "staffId": "staff-uuid-1",
    "staffName": "Nguyễn Văn A"
  }
}
```

---

#### 8. Ngừng hoạt động trạm sạc

**Endpoint**: `PATCH /api/stations/{stationId}/deactivate`

**Mô tả**: Đặt trạng thái trạm sạc thành OUT_OF_SERVICE.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0281234567",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "status": "OUT_OF_SERVICE",
    "active": false,
    "staffId": "staff-uuid-1",
    "staffName": "Nguyễn Văn A"
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
| 1006 | Not found | Không tìm thấy trạm sạc |

---

## Lưu ý khi sử dụng

1. **Tọa độ (latitude, longitude)**: Nếu không cung cấp, hệ thống sẽ tự động geocode từ địa chỉ
2. **Charging Points**: Khi tạo trạm mới, hệ thống tự động tạo số lượng charging points theo `numberOfChargingPoints`
3. **Staff Assignment**: Một trạm chỉ có thể gán cho một staff quản lý
4. **Status vs Active**: `active` là computed field, true khi `status` == OPERATIONAL
5. **Power Output**: Tất cả charging points trong trạm sẽ có cùng công suất được chỉ định
