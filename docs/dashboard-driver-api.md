# Dashboard API Documentation (Driver)

## Tổng quan

API Dashboard cho DRIVER hiển thị thống kê và phân tích về hoạt động sạc xe của họ, bao gồm tổng quan, biểu đồ theo giờ, trạm yêu thích và thói quen sạc.

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
    "totalSessions": 25,
    "totalEnergyKwh": 450.5,
    "totalCost": 2250000.0,
    "averageCostPerSession": 90000.0,
    "totalDuration": 1500,
    "averageDuration": 60,
    "periodStart": "2025-10-01T00:00:00",
    "periodEnd": "2025-10-31T23:59:59"
  }
}
```

**Response Fields**:
- `totalSessions` (integer): Tổng số phiên sạc
- `totalEnergyKwh` (number): Tổng năng lượng đã sạc (kWh)
- `totalCost` (number): Tổng chi phí (VNĐ)
- `averageCostPerSession` (number): Chi phí trung bình mỗi phiên
- `totalDuration` (integer): Tổng thời gian sạc (phút)
- `averageDuration` (integer): Thời gian trung bình mỗi phiên (phút)
- `periodStart` (string): Ngày bắt đầu khoảng thời gian
- `periodEnd` (string): Ngày kết thúc khoảng thời gian

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
- `lastVisit` (string): Lần sạc gần nhất
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
    "averageSocStart": 25,
    "averageSocEnd": 85,
    "totalDistinctStations": 8,
    "monthlyTrend": [
      {
        "month": "2025-08",
        "sessionCount": 20,
        "totalEnergy": 360.0,
        "totalCost": 1800000.0
      },
      {
        "month": "2025-09",
        "sessionCount": 23,
        "totalEnergy": 415.0,
        "totalCost": 2075000.0
      },
      {
        "month": "2025-10",
        "sessionCount": 25,
        "totalEnergy": 450.5,
        "totalCost": 2250000.0
      }
    ]
  }
}
```

**Response Fields**:
- `averageSessionsPerWeek` (number): Số phiên sạc trung bình mỗi tuần
- `averageEnergyPerSession` (number): Năng lượng trung bình mỗi phiên (kWh)
- `mostActiveDay` (string): Ngày trong tuần sạc nhiều nhất
- `mostActiveHour` (integer): Giờ trong ngày sạc nhiều nhất
- `preferredConnectorType` (string): Loại đầu sạc ưa thích
- `averageSocStart` (integer): SOC trung bình khi bắt đầu sạc (%)
- `averageSocEnd` (integer): SOC trung bình khi kết thúc sạc (%)
- `totalDistinctStations` (integer): Số trạm khác nhau đã sử dụng
- `monthlyTrend` (array): Xu hướng theo tháng (3 tháng gần nhất)

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
   - `today`: Chỉ thống kê hôm nay
   - `week`: 7 ngày gần nhất
   - `month`: 30 ngày gần nhất

2. **Caching**:
   - Dashboard data có thể được cache 5-10 phút để tối ưu performance
   - Dữ liệu realtime có thể có độ trễ nhỏ

3. **Empty Data**:
   - Nếu user mới, chưa có session nào → trả về giá trị 0
   - Favorite stations: trả về array rỗng nếu chưa có

4. **Timezone**:
   - Tất cả datetime đều theo UTC
   - Frontend cần convert sang timezone local

5. **Chart Visualization**:
   - Hourly sessions: dùng cho bar chart hoặc line chart
   - Monthly trend: dùng cho line chart hoặc area chart
   - Favorite stations: dùng cho table hoặc list view

6. **Performance**:
   - API đã được optimize với index trên database
   - Limit số lượng dữ liệu trả về để tránh quá tải
   - Sử dụng pagination nếu cần load thêm dữ liệu

