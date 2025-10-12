# API Documentation - Driver Dashboard

## 🎯 Tổng quan
Đây là các API cần thiết để xây dựng giao diện Dashboard cho Driver như trong hình bạn cung cấp.

## 📋 Các API đã tạo

### 1. Lấy Dashboard Overview
**Endpoint:** `GET /api/charging-sessions/my-dashboard`

**Mô tả:** Lấy thông tin tổng quan cho driver bao gồm:
- Tổng chi phí đã tiêu
- Tổng năng lượng đã sạc (kWh)
- Số phiên sạc
- Trung bình chi phí/tháng
- Thông tin xe và % pin hiện tại

**Quyền truy cập:** DRIVER

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalCost": 727690.0,
    "totalEnergyKwh": 212.9,
    "totalSessions": 5,
    "averageCostPerMonth": "3418",
    "vehicleModel": "Tesla Model 3",
    "licensePlate": "30A-12345",
    "currentBatterySoc": 75
  }
}
```

---

### 2. Lấy Lịch Sử Phiên Sạc
**Endpoint:** `GET /api/charging-sessions/my-sessions`

**Mô tả:** Lấy danh sách tất cả phiên sạc của driver, sắp xếp theo thời gian mới nhất trước.

**Quyền truy cập:** DRIVER

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "sessionId": "abc-123",
      "startTime": "2024-10-03T14:30:00",
      "endTime": "2024-10-03T15:15:00",
      "durationMin": 45,
      "stationName": "Vincom Đồng Khởi",
      "stationAddress": "72 Lê Thánh Tôn, Q1, TP.HCM",
      "chargingPointName": "Point A1",
      "startSocPercent": 30,
      "endSocPercent": 75,
      "energyKwh": 32.5,
      "costTotal": 113750.0,
      "status": "COMPLETED",
      "vehicleModel": "Tesla Model 3",
      "licensePlate": "30A-12345"
    }
  ]
}
```

---

### 3. Lấy Chi Tiết Phiên Sạc
**Endpoint:** `GET /api/charging-sessions/{sessionId}`

**Mô tả:** Lấy thông tin chi tiết của một phiên sạc cụ thể.

**Quyền truy cập:** DRIVER (chỉ xem được phiên sạc của mình)

**Response:** Tương tự như item trong danh sách ở API #2

---

## 🗂️ Các file đã tạo mới

### DTO Response:
1. **DriverDashboardResponse.java** - Response cho dashboard overview
2. **ChargingSessionResponse.java** - Response cho từng phiên sạc

### Service Layer:
3. **ChargingSessionService.java** - Logic xử lý business cho phiên sạc

### Controller:
4. **ChargingSessionController.java** - Expose các REST API endpoints

### Repository:
5. Đã bổ sung các query methods vào **ChargingSessionRepository.java**:
   - `sumTotalEnergyByDriverId()` - Tính tổng năng lượng
   - `findByDriverIdOrderByStartTimeDesc()` - Lấy danh sách sessions
   - `findLatestEndSocByDriverId()` - Lấy % pin gần nhất

### Entity:
6. Đã bổ sung trường `pointName` vào **ChargingPoint.java**

### Exception:
7. Đã thêm error code `SESSION_NOT_FOUND` vào **ErrorCode.java**

---

## 🧪 Test API

### Yêu cầu:
1. Đăng nhập với tài khoản DRIVER
2. Lấy access token từ response
3. Thêm token vào header: `Authorization: Bearer {token}`

### Ví dụ test với cURL:

```bash
# 1. Lấy dashboard
curl -X GET "http://localhost:8080/evchargingstation/api/charging-sessions/my-dashboard" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. Lấy lịch sử
curl -X GET "http://localhost:8080/evchargingstation/api/charging-sessions/my-sessions" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Lấy chi tiết session
curl -X GET "http://localhost:8080/evchargingstation/api/charging-sessions/{sessionId}" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 📊 Database Schema

Đảm bảo bạn đã có dữ liệu mẫu trong các bảng:
- `drivers` - Thông tin driver
- `vehicles` - Xe của driver
- `charging_sessions` - Lịch sử phiên sạc
- `charging_points` - Điểm sạc (cần có `point_name`)
- `stations` - Trạm sạc

---

## ✅ Checklist hoàn thành

- [x] DTO Response cho Dashboard và Session
- [x] Service layer xử lý business logic
- [x] Controller expose API endpoints
- [x] Repository queries cho driver data
- [x] Error handling
- [x] Security với role DRIVER
- [x] Sorting sessions theo thời gian mới nhất

---

## 🎨 Frontend Integration

Để tích hợp với giao diện bạn đã cung cấp:

1. **Header Section**: Call API `/my-dashboard` để lấy thông tin tổng quan và thông tin xe
2. **Transaction Table**: Call API `/my-sessions` để lấy danh sách lịch sử
3. **Detail Modal**: Call API `/charging-sessions/{id}` khi click vào từng dòng

---

## 🔒 Bảo mật

- Tất cả API đều yêu cầu authentication
- Chỉ có role DRIVER mới truy cập được
- Driver chỉ xem được dữ liệu của chính mình
- Token JWT bắt buộc trong header


