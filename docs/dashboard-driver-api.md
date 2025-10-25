# Dashboard API Documentation (Driver)

## Tổng quan

API Dashboard cho DRIVER hiển thị thống kê và phân tích về hoạt động sạc xe của họ.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: DRIVER

---

## API Endpoints

### 1. Lấy thống kê tổng quan

**Endpoint**: `GET /api/dashboard/summary`

**Mô tả**: Lấy thống kê tổng quan về hoạt động sạc (cards trên cùng của dashboard).

**Quyền truy cập**: DRIVER

**Query Parameters**:
- `period` (string, optional): Khoảng thời gian thống kê (today | week | month), mặc định: "month"

**Example Request**:
```
GET /api/dashboard/summary?period=month
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "totalRevenue": 2250000.0,
    "totalEnergyUsed": 450.5,
    "totalSessions": 25,
    "averagePricePerKwh": 5000.0
  }
}
```

**Response Fields**:
- `totalRevenue` (number): Tổng chi phí đã trả (VNĐ)
- `totalEnergyUsed` (number): Tổng năng lượng đã sạc (kWh)
- `totalSessions` (integer): Tổng số phiên sạc
- `averagePricePerKwh` (number): Giá trung bình mỗi kWh (VNĐ)

---

### 2. Lấy thống kê theo giờ trong ngày

**Endpoint**: `GET /api/dashboard/hourly-sessions`

**Mô tả**: Lấy số lượng phiên sạc theo từng giờ trong ngày (dùng cho biểu đồ).

**Quyền truy cập**: DRIVER

**Query Parameters**:
- `date` (string, optional): Ngày cần thống kê (format: yyyy-MM-dd), mặc định: hôm nay

**Example Request**:
```
GET /api/dashboard/hourly-sessions?date=2025-10-25
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "hour": 0,
      "sessionCount": 0,
      "totalEnergyKwh": 0.0,
      "totalCost": 0.0
    },
    {
      "hour": 6,
      "sessionCount": 2,
      "totalEnergyKwh": 35.5,
      "totalCost": 177500.0
    },
    {
      "hour": 8,
      "sessionCount": 3,
      "totalEnergyKwh": 48.0,
      "totalCost": 240000.0
    },
    {
      "hour": 12,
      "sessionCount": 1,
      "totalEnergyKwh": 20.0,
      "totalCost": 100000.0
    },
    {
      "hour": 18,
      "sessionCount": 4,
      "totalEnergyKwh": 65.0,
      "totalCost": 325000.0
    }
  ]
}
```

**Response Fields** (mỗi item):
- `hour` (integer): Giờ trong ngày (0-23)
- `sessionCount` (integer): Số phiên sạc trong giờ đó
- `totalEnergyKwh` (number): Tổng năng lượng sạc trong giờ đó
- `totalCost` (number): Tổng chi phí trong giờ đó

**Lưu ý**: 
- Trả về đủ 24 giờ, những giờ không có session sẽ có count = 0
- Dùng để vẽ biểu đồ cột/line chart

---

### 3. Lấy danh sách trạm yêu thích

**Endpoint**: `GET /api/dashboard/favorite-stations`

**Mô tả**: Lấy danh sách trạm sạc mà driver thường xuyên sử dụng nhất.

**Quyền truy cập**: DRIVER

**Query Parameters**:
- `limit` (integer, optional): Số lượng trạm trả về, mặc định: 5

**Example Request**:
```
GET /api/dashboard/favorite-stations?limit=5
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "stationName": "Trạm sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "visitCount": 15,
      "totalEnergyKwh": 250.5,
      "totalCost": 1250000.0,
      "lastVisit": "2025-10-24T18:30:00",
      "averageDuration": 55
    },
    {
      "stationId": "station-uuid-2",
      "stationName": "Trạm sạc Quận 7",
      "address": "456 Nguyễn Văn Linh, Quận 7, TP.HCM",
      "visitCount": 8,
      "totalEnergyKwh": 140.0,
      "totalCost": 700000.0,
      "lastVisit": "2025-10-23T12:15:00",
      "averageDuration": 48
    }
  ]
}
```

**Response Fields** (mỗi station):
- `stationId` (string): ID của trạm
- `stationName` (string): Tên trạm
- `address` (string): Địa chỉ
- `visitCount` (integer): Số lần sử dụng
- `totalEnergyKwh` (number): Tổng năng lượng đã sạc tại trạm này
- `totalCost` (number): Tổng chi phí tại trạm này
- `lastVisit` (string): Lần sạc gần nhất (ISO datetime)
- `averageDuration` (integer): Thời gian sạc trung bình (phút)

---

### 4. Lấy thống kê thói quen sạc

**Endpoint**: `GET /api/dashboard/charging-statistics`

**Mô tả**: Lấy thống kê chi tiết về thói quen sạc của driver.

**Quyền truy cập**: DRIVER

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "averageSessionsPerWeek": 6.2,
    "averageEnergyPerSession": 18.5,
    "mostActiveDay": "Monday",
    "mostActiveHour": 18,
    "preferredConnectorType": "CCS2",
    "totalLifetimeSessions": 125,
    "totalLifetimeEnergy": 2312.5,
    "totalLifetimeCost": 11562500.0
  }
}
```

**Response Fields**:
- `averageSessionsPerWeek` (number): Trung bình số phiên sạc mỗi tuần
- `averageEnergyPerSession` (number): Năng lượng trung bình mỗi phiên (kWh)
- `mostActiveDay` (string): Ngày trong tuần thường sạc nhất (Monday, Tuesday, ...)
- `mostActiveHour` (integer): Giờ trong ngày thường sạc nhất (0-23)
- `preferredConnectorType` (string): Loại connector thường dùng nhất
- `totalLifetimeSessions` (integer): Tổng số phiên sạc từ trước đến nay
- `totalLifetimeEnergy` (number): Tổng năng lượng đã sạc từ trước đến nay (kWh)
- `totalLifetimeCost` (number): Tổng chi phí từ trước đến nay (VNĐ)

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập |

---

## Lưu ý khi sử dụng

1. **Period Parameter**: 
   - `today`: Thống kê trong ngày hôm nay
   - `week`: Thống kê 7 ngày gần nhất
   - `month`: Thống kê 30 ngày gần nhất

2. **Hourly Sessions**: 
   - Luôn trả về đủ 24 giờ
   - Những giờ không có dữ liệu sẽ có giá trị 0
   - Dùng để vẽ biểu đồ

3. **Favorite Stations**: 
   - Sắp xếp theo số lần sử dụng giảm dần
   - Mặc định trả về top 5 trạm

4. **Charging Statistics**: 
   - Thống kê toàn bộ lịch sử sạc của driver
   - Dùng để phân tích thói quen và xu hướng
