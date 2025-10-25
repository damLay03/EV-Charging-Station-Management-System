# Admin Dashboard API Documentation

## Tổng quan

API Dashboard cho ADMIN quản lý toàn bộ hệ thống, xem báo cáo doanh thu, thống kê sử dụng trạm và tổng quan hệ thống.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: ADMIN

---

## API Endpoints

### SYSTEM OVERVIEW

#### 1. Lấy tổng quan hệ thống

**Endpoint**: `GET /api/overview`

**Mô tả**: Lấy thống kê tổng quan toàn hệ thống (số trạm, điểm sạc, người dùng, doanh thu).

**Quyền truy cập**: ADMIN

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "totalStations": 25,
    "totalChargingPoints": 150,
    "activeChargingPoints": 120,
    "totalDrivers": 1250,
    "currentMonthRevenue": 125000000.0,
    "currentMonthSessions": 3500,
    "averageSessionDuration": 55,
    "averageRevenuePerSession": 35714.0
  }
}
```

**Response Fields**:
- `totalStations` (integer): Tổng số trạm sạc
- `totalChargingPoints` (integer): Tổng số điểm sạc
- `activeChargingPoints` (integer): Số điểm sạc đang hoạt động
- `totalDrivers` (integer): Tổng số người dùng (driver)
- `currentMonthRevenue` (number): Doanh thu tháng hiện tại (VNĐ)
- `currentMonthSessions` (integer): Số phiên sạc tháng hiện tại
- `averageSessionDuration` (integer): Thời gian trung bình mỗi phiên (phút)
- `averageRevenuePerSession` (number): Doanh thu trung bình mỗi phiên (VNĐ)

---

### REVENUE ANALYTICS

#### 2. Lấy doanh thu theo tuần

**Endpoint**: `GET /api/revenue/weekly`

**Mô tả**: Lấy doanh thu từng trạm sạc theo tuần.

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `year` (integer, optional): Năm cần thống kê (mặc định: năm hiện tại)
- `month` (integer, optional): Tháng (1-12, mặc định: tháng hiện tại)
- `week` (integer, optional): Tuần trong tháng (1-5, mặc định: tuần hiện tại)

**Example Request**:
```
GET /api/revenue/weekly?year=2025&month=10&week=4
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "stationName": "Trạm sạc Quận 1",
      "totalRevenue": 5000000.0,
      "totalSessions": 150,
      "totalEnergyKwh": 2500.0,
      "averageRevenuePerSession": 33333.0,
      "periodStart": "2025-10-21",
      "periodEnd": "2025-10-27"
    },
    {
      "stationId": "station-uuid-2",
      "stationName": "Trạm sạc Quận 7",
      "totalRevenue": 3500000.0,
      "totalSessions": 100,
      "totalEnergyKwh": 1750.0,
      "averageRevenuePerSession": 35000.0,
      "periodStart": "2025-10-21",
      "periodEnd": "2025-10-27"
    }
  ]
}
```

**Response Fields** (mỗi station):
- `stationId` (string): ID của trạm
- `stationName` (string): Tên trạm
- `totalRevenue` (number): Tổng doanh thu (VNĐ)
- `totalSessions` (integer): Số phiên sạc
- `totalEnergyKwh` (number): Tổng năng lượng (kWh)
- `averageRevenuePerSession` (number): Doanh thu trung bình/phiên
- `periodStart` (string): Ngày bắt đầu tuần
- `periodEnd` (string): Ngày kết thúc tuần

---

#### 3. Lấy doanh thu theo tháng

**Endpoint**: `GET /api/revenue/monthly`

**Mô tả**: Lấy doanh thu từng trạm sạc theo tháng.

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `year` (integer, optional): Năm cần thống kê (mặc định: năm hiện tại)
- `month` (integer, optional): Tháng (1-12, mặc định: tháng hiện tại)

**Example Request**:
```
GET /api/revenue/monthly?year=2025&month=10
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "stationName": "Trạm sạc Quận 1",
      "totalRevenue": 25000000.0,
      "totalSessions": 750,
      "totalEnergyKwh": 12500.0,
      "averageRevenuePerSession": 33333.0,
      "periodStart": "2025-10-01",
      "periodEnd": "2025-10-31"
    }
  ]
}
```

---

#### 4. Lấy doanh thu theo năm

**Endpoint**: `GET /api/revenue/yearly`

**Mô tả**: Lấy doanh thu từng trạm sạc theo năm (tất cả các tháng).

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `year` (integer, optional): Năm cần thống kê (mặc định: năm hiện tại)

**Example Request**:
```
GET /api/revenue/yearly?year=2025
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "stationName": "Trạm sạc Quận 1",
      "totalRevenue": 300000000.0,
      "totalSessions": 9000,
      "totalEnergyKwh": 150000.0,
      "averageRevenuePerSession": 33333.0,
      "periodStart": "2025-01-01",
      "periodEnd": "2025-12-31"
    }
  ]
}
```

---

### STATION USAGE ANALYTICS

#### 5. Lấy mức độ sử dụng một trạm hôm nay

**Endpoint**: `GET /api/station-usage/{stationId}/today`

**Mô tả**: Xem mức độ sử dụng của một trạm cụ thể trong ngày hôm nay.

**Quyền truy cập**: ADMIN, STAFF

**Path Parameters**:
- `stationId` (string, required): ID của trạm

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "stationId": "station-uuid-1",
    "stationName": "Trạm sạc Quận 1",
    "date": "2025-10-25",
    "totalChargingPoints": 8,
    "totalSessions": 35,
    "averageUsagePercent": 62.5,
    "peakHour": 18,
    "peakUsagePercent": 87.5,
    "hourlyUsage": [
      {
        "hour": 0,
        "sessionCount": 0,
        "usagePercent": 0.0
      },
      {
        "hour": 6,
        "sessionCount": 2,
        "usagePercent": 25.0
      },
      {
        "hour": 18,
        "sessionCount": 7,
        "usagePercent": 87.5
      }
    ]
  }
}
```

**Response Fields**:
- `stationId` (string): ID của trạm
- `stationName` (string): Tên trạm
- `date` (string): Ngày thống kê
- `totalChargingPoints` (integer): Tổng số trụ sạc
- `totalSessions` (integer): Tổng số phiên trong ngày
- `averageUsagePercent` (number): % sử dụng trung bình
- `peakHour` (integer): Giờ cao điểm
- `peakUsagePercent` (number): % sử dụng cao điểm
- `hourlyUsage` (array): Thống kê theo giờ

---

#### 6. Lấy mức độ sử dụng một trạm theo ngày

**Endpoint**: `GET /api/station-usage/{stationId}`

**Mô tả**: Xem mức độ sử dụng của một trạm cụ thể trong ngày chỉ định.

**Quyền truy cập**: ADMIN, STAFF

**Path Parameters**:
- `stationId` (string, required): ID của trạm

**Query Parameters**:
- `date` (string, optional): Ngày cần xem (format: yyyy-MM-dd), mặc định: hôm nay

**Example Request**:
```
GET /api/station-usage/station-uuid-1?date=2025-10-20
```

**Response**: Giống endpoint #5

---

#### 7. Lấy mức độ sử dụng tất cả trạm hôm nay

**Endpoint**: `GET /api/station-usage/all/today`

**Mô tả**: Xem mức độ sử dụng của tất cả trạm trong ngày hôm nay.

**Quyền truy cập**: ADMIN

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "station-uuid-1",
      "stationName": "Trạm sạc Quận 1",
      "date": "2025-10-25",
      "totalChargingPoints": 8,
      "totalSessions": 35,
      "averageUsagePercent": 62.5,
      "peakHour": 18,
      "peakUsagePercent": 87.5
    },
    {
      "stationId": "station-uuid-2",
      "stationName": "Trạm sạc Quận 7",
      "date": "2025-10-25",
      "totalChargingPoints": 10,
      "totalSessions": 40,
      "averageUsagePercent": 55.0,
      "peakHour": 17,
      "peakUsagePercent": 80.0
    }
  ]
}
```

---

#### 8. Lấy mức độ sử dụng tất cả trạm theo ngày

**Endpoint**: `GET /api/station-usage/all`

**Mô tả**: Xem mức độ sử dụng của tất cả trạm trong ngày chỉ định.

**Quyền truy cập**: ADMIN

**Query Parameters**:
- `date` (string, optional): Ngày cần xem (format: yyyy-MM-dd), mặc định: hôm nay

**Example Request**:
```
GET /api/station-usage/all?date=2025-10-20
```

**Response**: Giống endpoint #7

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập (không phải ADMIN) |
| 1006 | Not found | Không tìm thấy trạm |

---

## Lưu ý khi sử dụng

1. **Date Parameters**:
   - Tất cả date parameter đều theo format ISO 8601: `yyyy-MM-dd`
   - Nếu không truyền, mặc định lấy giá trị hiện tại (hôm nay, tháng này, năm nay)

2. **Week Calculation**:
   - Tuần 1: ngày 1-7 của tháng
   - Tuần 2: ngày 8-14
   - Tuần 3: ngày 15-21
   - Tuần 4: ngày 22-28
   - Tuần 5: ngày 29-31 (nếu có)

3. **Revenue Aggregation**:
   - Revenue chỉ tính từ các session có status = COMPLETED
   - Payment status không ảnh hưởng đến số liệu thống kê
   - Sử dụng plan mặc định để tính toán

4. **Usage Percentage Calculation**:
   - usagePercent = (số trụ đang sử dụng / tổng số trụ) * 100
   - Chỉ tính trụ có status = OCCUPIED
   - Trụ OUT_OF_ORDER không tính vào tổng

5. **Chart Visualization**:
   - Weekly/Monthly/Yearly revenue: dùng cho bar chart hoặc line chart so sánh giữa các trạm
   - Hourly usage: dùng cho line chart theo thời gian
   - System overview: dùng cho cards/KPI dashboard

6. **Performance**:
   - Revenue data được cache 1 giờ
   - Usage data được cache 5 phút
   - Có thể thêm query parameter `refresh=true` để force refresh cache

7. **Export Data**:
   - Có thể thêm parameter `format=csv` để export dữ liệu
   - Hoặc frontend tự xử lý export từ JSON response

8. **Timezone**:
   - Tất cả datetime đều theo UTC
   - Week/Month/Year boundaries theo UTC timezone
   - Frontend cần convert sang local timezone khi hiển thị
