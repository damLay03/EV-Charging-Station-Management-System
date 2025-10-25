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
- `OCCUPIED`: Đang được sử dụng
- `OUT_OF_ORDER`: Hỏng hóc
- `RESERVED`: Đã được đặt trước

### ConnectorType
- `TYPE_2`: Chuẩn Type 2 (châu Âu)
- `CCS2`: Combined Charging System 2
- `CHADEMO`: Chuẩn CHAdeMO (Nhật Bản)
- `GB_T`: Chuẩn GB/T (Trung Quốc)

---

## API Endpoints

### STATION MANAGEMENT

#### 1. Lấy danh sách trạm sạc (Overview)

**Endpoint**: `GET /api/stations/overview`

**Mô tả**: Lấy danh sách tất cả trạm sạc với thông tin tổng quan (nhẹ hơn endpoint detail).

**Quyền truy cập**: ADMIN

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "status": "OPERATIONAL",
      "totalChargingPoints": 8,
      "availablePoints": 5,
      "active": true
    }
  ]
}
```

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
      "id": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "EV Charging Corp",
      "contactPhone": "0281234567",
      "status": "OPERATIONAL"
    }
  ]
}
```

---

#### 3. Lấy danh sách trạm sạc chi tiết (Detail)

**Endpoint**: `GET /api/stations/detail`

**Mô tả**: Lấy danh sách trạm sạc với thông tin đầy đủ (bao gồm điểm sạc, doanh thu, % sử dụng, nhân viên).

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `status` (string, optional): Lọc theo trạng thái

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "station-uuid-1",
      "name": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "EV Charging Corp",
      "contactPhone": "0281234567",
      "status": "OPERATIONAL",
      "totalChargingPoints": 8,
      "availablePoints": 5,
      "monthlyRevenue": 25000000.0,
      "usagePercentage": 62.5,
      "staffCount": 3,
      "staffNames": ["Nguyễn Văn A", "Trần Thị B"]
    }
  ]
}
```

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
  "operatorName": "EV Charging Corp",
  "contactPhone": "0287654321",
  "status": "OPERATIONAL",
  "staffIds": ["staff-uuid-1", "staff-uuid-2"]
}
```

**Request Fields**:
- `name` (string, required): Tên trạm sạc
- `address` (string, required): Địa chỉ đầy đủ
- `operatorName` (string, optional): Tên đơn vị vận hành
- `contactPhone` (string, optional): Số điện thoại liên hệ
- `status` (string, optional): Trạng thái ban đầu (mặc định: OPERATIONAL)
- `staffIds` (array, optional): Danh sách ID của staff được gán

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "station-uuid-new",
    "name": "Trạm sạc Quận 7",
    "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0287654321",
    "status": "OPERATIONAL"
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
  "status": "UNDER_MAINTENANCE"
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "station-uuid-1",
    "name": "Trạm sạc Quận 7 - Updated",
    "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
    "operatorName": "EV Charging Corp",
    "contactPhone": "0287654321",
    "status": "UNDER_MAINTENANCE"
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
    "id": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "status": "OUT_OF_SERVICE"
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
    "id": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "status": "OPERATIONAL"
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
    "id": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "status": "OUT_OF_SERVICE"
  }
}
```

---

#### 9. Toggle trạng thái trạm sạc

**Endpoint**: `PATCH /api/stations/{stationId}/toggle`

**Mô tả**: Chuyển đổi trạng thái giữa OPERATIONAL và OUT_OF_SERVICE.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "station-uuid-1",
    "name": "Trạm sạc Quận 1",
    "status": "OPERATIONAL"
  }
}
```

---

#### 10. Xóa trạm sạc

**Endpoint**: `DELETE /api/stations/{stationId}`

**Mô tả**: Xóa trạm sạc khỏi hệ thống. Các charging points liên quan sẽ tự động bị xóa (cascade).

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Station deleted successfully"
}
```

---

### STAFF MANAGEMENT

#### 11. Lấy danh sách tất cả nhân viên

**Endpoint**: `GET /api/stations/staff/all`

**Mô tả**: Lấy danh sách tất cả staff để chọn khi tạo/cập nhật trạm sạc.

**Quyền truy cập**: ADMIN

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "staff-uuid-1",
      "fullName": "Nguyễn Văn A",
      "email": "staffa@example.com",
      "phoneNumber": "0901234567",
      "currentStationId": "station-uuid-1",
      "currentStationName": "Trạm sạc Quận 1"
    }
  ]
}
```

---

### CHARGING POINTS MANAGEMENT

#### 12. Thêm trụ sạc vào trạm

**Endpoint**: `POST /api/stations/{stationId}/charging-points`

**Mô tả**: Tạo thêm trụ sạc cho trạm sạc đã tồn tại.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Request Body**:
```json
{
  "name": "Trụ A1",
  "powerKw": 50.0,
  "connectorType": "CCS2",
  "pricePerKwh": 5000.0,
  "status": "AVAILABLE"
}
```

**Request Fields**:
- `name` (string, optional): Tên trụ sạc (nếu không cung cấp, hệ thống tự sinh)
- `powerKw` (number, required): Công suất (kW)
- `connectorType` (string, required): Loại đầu sạc (TYPE_2 | CCS2 | CHADEMO | GB_T)
- `pricePerKwh` (number, required): Giá mỗi kWh (VNĐ)
- `status` (string, optional): Trạng thái ban đầu (mặc định: AVAILABLE)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "cp-uuid-1",
    "name": "Trụ A1",
    "powerKw": 50.0,
    "connectorType": "CCS2",
    "pricePerKwh": 5000.0,
    "status": "AVAILABLE",
    "stationId": "station-uuid-1"
  },
  "message": "Charging point created successfully"
}
```

---

#### 13. Lấy danh sách trụ sạc của trạm

**Endpoint**: `GET /api/stations/{stationId}/charging-points`

**Mô tả**: Lấy tất cả trụ sạc của một trạm sạc.

**Quyền truy cập**: Public

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "cp-uuid-1",
      "name": "Trụ A1",
      "powerKw": 50.0,
      "connectorType": "CCS2",
      "pricePerKwh": 5000.0,
      "status": "AVAILABLE",
      "stationId": "station-uuid-1"
    },
    {
      "id": "cp-uuid-2",
      "name": "Trụ A2",
      "powerKw": 100.0,
      "connectorType": "CCS2",
      "pricePerKwh": 6000.0,
      "status": "OCCUPIED",
      "stationId": "station-uuid-1"
    }
  ]
}
```

---

#### 14. Cập nhật thông tin trụ sạc

**Endpoint**: `PUT /api/stations/{stationId}/charging-points/{chargingPointId}`

**Mô tả**: Cập nhật thông tin trụ sạc (status, price, power, connectorType).

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc
- `chargingPointId` (string, required): ID của trụ sạc

**Request Body**:
```json
{
  "powerKw": 75.0,
  "connectorType": "CCS2",
  "pricePerKwh": 5500.0,
  "status": "AVAILABLE"
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "cp-uuid-1",
    "name": "Trụ A1",
    "powerKw": 75.0,
    "connectorType": "CCS2",
    "pricePerKwh": 5500.0,
    "status": "AVAILABLE",
    "stationId": "station-uuid-1"
  }
}
```

---

#### 15. Xóa trụ sạc

**Endpoint**: `DELETE /api/stations/{stationId}/charging-points/{chargingPointId}`

**Mô tả**: Xóa trụ sạc khỏi trạm.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `stationId` (string, required): ID của trạm sạc
- `chargingPointId` (string, required): ID của trụ sạc

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Charging point deleted successfully"
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Station not found | Không tìm thấy trạm sạc |
| 1007 | Charging point not found | Không tìm thấy trụ sạc |

---

## Lưu ý

1. **Cascade Delete**: Khi xóa trạm sạc, tất cả trụ sạc thuộc trạm đó cũng bị xóa.

2. **Charging Point Naming**: Nếu không cung cấp tên khi tạo trụ sạc, hệ thống sẽ tự động đặt tên theo format: `{stationName}-CP-{số thứ tự}`.

3. **Status Management**: 
   - Trạm sạc có 3 trạng thái: OPERATIONAL, OUT_OF_SERVICE, UNDER_MAINTENANCE
   - Trụ sạc có 4 trạng thái: AVAILABLE, OCCUPIED, OUT_OF_ORDER, RESERVED

4. **Price Configuration**: Giá được tính theo VNĐ/kWh, có thể khác nhau cho từng trụ sạc.

5. **Power Levels**:
   - Sạc chậm (AC): 7-22 kW
   - Sạc nhanh (DC): 50-150 kW
   - Sạc siêu nhanh: 150-350 kW

