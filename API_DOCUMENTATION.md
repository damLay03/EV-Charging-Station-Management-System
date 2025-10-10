# EV Charging Station Management System – API Reference

Below is the full list of all HTTP endpoints in the system, organized by resource. For each endpoint you'll find:

- HTTP method and URL  
- Authentication requirement (`Public` or `Bearer token required`)  
- A minimal sample JSON response  

All responses are wrapped in the common `ApiResponse<T>` envelope:  
```json
{
  "code": 1000,
  "message": null,
  "result": { ... }
}
```

---

## 1. Authentication

### POST /api/auth/login  
Public (no token required)

Request body:
```json
{
  "email": "user@example.com",
  "password": "secret"
}
```

Sample successful response:
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJldi1jaGFyZ2luZy1zdGF0aW9uIiwic3ViIjoiYm9tdGh1QGFkbWluLmV2LmNvbSIsImV4cCI6MTc1OTY1ODkyMCwiaWF0IjoxNzU5NjU1MzIwLCJzY29wZSI6IkFETUlOIn0.QXE9Dsbdc-b41T283x89huoewTMJ7x13_tjuEL-Vkr6X5b2aR45kBtxFM7R8LPdU82WFt7Y40c9SzLEaSEwUZQ",
    "authenticated": true,
    "userInfo": {
      "userId": "1ee30c49-7c50-4b8f-a5f7-40e0646fe742",
      "email": "user@example.com",
      "phone": "...",
      "dateOfBirth": "...",
      "gender": false,
      "firstName": "...",
      "lastName": "...",
      "fullName": "...",
      "role": "..."
    }
  }
}
```

---

## 2. Users

### POST /api/users/register  
Public (no token required)  
Register a new **driver**.

Request body:
```json
{
  "email": "newdriver@example.com",
  "password": "hunter2",
  "confirmPassword": "hunter2"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "newdriver@example.com",
    "phone": null,
    "dateOfBirth": null,
    "gender": false,
    "firstName": null,
    "lastName": null,
    "fullName": null,
    "role": "DRIVER"
  }
}
```

### GET /api/users/driver/myInfo  
Bearer token required (ROLE_DRIVER)  
Get the authenticated driver's profile với đầy đủ thông tin từ User và Driver entity.

Sample response (`DriverResponse`):
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "driver@example.com",
    "phone": "0123456789",
    "dateOfBirth": "1990-01-01",
    "gender": true,
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "role": "DRIVER",
    "address": "123 Main Street, Hanoi",
    "joinDate": "2025-09-15T10:30:00"
  }
}
```

### PATCH /api/users/driver/myInfo  
Bearer token required (ROLE_DRIVER)  
Partially update the authenticated driver's profile. Có thể cập nhật: phone, dateOfBirth, gender, firstName, lastName, **address**.

Request body (`UserUpdateRequest`) - tất cả fields đều optional:
```json
{
  "phone": "0987654321",
  "dateOfBirth": "1990-01-01",
  "gender": true,
  "firstName": "John",
  "lastName": "Doe",
  "address": "456 New Address, HCMC"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "driver@example.com",
    "phone": "0987654321",
    "dateOfBirth": "1990-01-01",
    "gender": true,
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "role": "DRIVER",
    "address": "456 New Address, HCMC",
    "joinDate": "2025-09-15T10:30:00"
  }
}
```

### GET /api/users/{userId}  
Bearer token required  
Retrieve any one user's details (for driver self or admin).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "someone@example.com",
    "phone": "0123456789",
    "dateOfBirth": "1990-01-01",
    "gender": false,
    "firstName": "Jane",
    "lastName": "Smith",
    "fullName": "Jane Smith",
    "role": "DRIVER"
  }
}
```

### GET /api/users  
Bearer token required (ROLE_ADMIN)  
List all drivers with admin view (`AdminUserResponse`).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "fullName": "John Doe",
      "email": "driver1@example.com",
      "phone": "0123456789",
      "joinDate": "2025-09-01",
      "planName": "Premium",
      "sessionCount": 5,
      "totalSpent": 150.75,
      "status": "Hoạt động",
      "isActive": true
    }
  ]
}
```

### GET /api/users/driver/{driverId}/info  
Bearer token required (ROLE_ADMIN)  
Admin lấy thông tin đầy đủ của một driver (bao gồm address và joinDate).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "driver@example.com",
    "phone": "0123456789",
    "dateOfBirth": "1990-01-01",
    "gender": true,
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "role": "DRIVER",
    "address": "123 Main Street, Hanoi",
    "joinDate": "2025-09-15T10:30:00"
  }
}
```

### PUT /api/users/driver/{driverId}  
Bearer token required (ROLE_ADMIN)  
Admin cập nhật thông tin driver. **Không thể sửa email, password, joinDate.**

Request body (`AdminUpdateDriverRequest`) - tất cả fields đều optional:
```json
{
  "phone": "0987654321",
  "dateOfBirth": "1990-01-01",
  "gender": true,
  "firstName": "John",
  "lastName": "Doe",
  "address": "456 Updated Address, HCMC"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "userId": "uuid-1234-...",
    "email": "driver@example.com",
    "phone": "0987654321",
    "dateOfBirth": "1990-01-01",
    "gender": true,
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "role": "DRIVER",
    "address": "456 Updated Address, HCMC",
    "joinDate": "2025-09-15T10:30:00"
  }
}
```

### DELETE /api/users/{userId}  
Bearer token required (ROLE_ADMIN)  
Hard delete a user. Returns default success code and message:

Sample response:
```json
{
  "code": 1000,
  "message": "Deleted",
  "result": null
}
```

---

## 3. System Overview

### GET /api/overview  
Bearer token required (ROLE_ADMIN)  
Lấy dữ liệu tổng quan hệ thống bao gồm: tổng số trạm sạc, điểm sạc đang hoạt động, tổng số driver, doanh thu tháng hiện tại.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "totalStations": 12,
    "activeChargingPoints": 8,
    "totalDrivers": 42,
    "currentMonthRevenue": 3210.50
  }
}
```

### GET /api/overview/total-stations  
Bearer token required (ROLE_ADMIN)  
Lấy tổng số trạm sạc trong hệ thống.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": 12
}
```

### GET /api/overview/total-drivers  
Bearer token required (ROLE_ADMIN)  
Lấy tổng số driver trong hệ thống.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": 42
}
```

---

## 4. Plans

All plan endpoints require bearer token (ROLE_ADMIN for create operations, GET endpoints are public).

### POST /api/plans  
Bearer token required (ROLE_ADMIN)  
Tạo plan generic theo billingType trong body.

Request body (`PlanCreationRequest`):
```json
{
  "name": "Basic Plan",
  "billingType": "MONTHLY_SUBSCRIPTION",
  "pricePerKwh": 0.1,
  "pricePerMinute": 0.02,
  "monthlyFee": 9.99,
  "benefits": "Free parking"
}
```

### POST /api/plans/prepaid  
Bearer token required (ROLE_ADMIN)  
Tạo plan PREPAID (override billingType).

### POST /api/plans/postpaid  
Bearer token required (ROLE_ADMIN)  
Tạo plan POSTPAID (override billingType).

### POST /api/plans/vip  
Bearer token required (ROLE_ADMIN)  
Tạo plan VIP (override billingType, yêu cầu monthlyFee > 0).

### GET /api/plans  
Public access  
Lấy tất cả plan.

Sample `PlanResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "planId": "uuid-5678-...",
      "name": "Basic Plan",
      "billingType": "MONTHLY_SUBSCRIPTION",
      "pricePerKwh": 0.1,
      "pricePerMinute": 0.02,
      "monthlyFee": 9.99,
      "benefits": "Free parking"
    }
  ]
}
```

### GET /api/plans/{planId}  
Bearer token required (ROLE_ADMIN)  
Lấy chi tiết 1 plan theo id.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "planId": "uuid-5678-...",
    "name": "Basic Plan",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 0.1,
    "pricePerMinute": 0.02,
    "monthlyFee": 9.99,
    "benefits": "Free parking"
  }
}
```

### PUT /api/plans/{planId}  
Bearer token required (ROLE_ADMIN)  
Cập nhật plan theo id. Validate name unique (trừ chính nó) và config theo billingType mới.

Request body (`PlanUpdateRequest`):
```json
{
  "name": "Premium Plan",
  "billingType": "VIP",
  "pricePerKwh": 0.08,
  "pricePerMinute": 0.015,
  "monthlyFee": 29.99,
  "benefits": "Free parking + Priority charging"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "planId": "uuid-5678-...",
    "name": "Premium Plan",
    "billingType": "VIP",
    "pricePerKwh": 0.08,
    "pricePerMinute": 0.015,
    "monthlyFee": 29.99,
    "benefits": "Free parking + Priority charging"
  }
}
```

### DELETE /api/plans/{planId}  
Bearer token required (ROLE_ADMIN)  
Xóa plan theo id.

Sample response:
```json
{
  "code": 1000,
  "message": "Plan deleted successfully",
  "result": null
}
```

**Plan Validation Rules:**
- **PREPAID**: `monthlyFee` phải = 0, phải có ít nhất một trong `pricePerKwh` hoặc `pricePerMinute` > 0
- **POSTPAID**: `monthlyFee` phải = 0, phải có ít nhất một trong `pricePerKwh` hoặc `pricePerMinute` > 0
- **VIP**: `monthlyFee` phải > 0
- **MONTHLY_SUBSCRIPTION**: `monthlyFee` phải > 0
- **PAY_AS_YOU_GO**: `monthlyFee` phải = 0

**Error Codes:**
- `3001`: Plan Not Found
- `3002`: Plan Name Existed (trùng tên, case-insensitive)
- `3003`: Invalid Plan Configuration (vi phạm rule validation theo billingType)

---

## 5. Revenue

All revenue endpoints require bearer token (ROLE_ADMIN).

### GET /api/revenue/weekly?year={year}&month={month}&week={week}  
Lấy thống kê doanh thu theo tuần của từng trạm sạc.

Query parameters:
- `year` (optional): Năm cần thống kê (mặc định: năm hiện tại)
- `month` (optional): Tháng cần thống kê (mặc định: tháng hiện tại)
- `week` (optional): Tuần cần thống kê (mặc định: tuần hiện tại)

### GET /api/revenue/monthly?year={year}&month={month}  
Lấy thống kê doanh thu theo tháng của từng trạm sạc.

Query parameters:
- `year` (optional): Năm cần thống kê (mặc định: năm hiện tại)
- `month` (optional): Tháng cần thống kê (mặc định: tháng hiện tại)

### GET /api/revenue/yearly?year={year}  
Lấy thống kê doanh thu theo năm của từng trạm sạc (tất cả các tháng).

Query parameters:
- `year` (optional): Năm cần thống kê (mặc định: năm hiện tại)

Sample `StationRevenueResponse` list:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "stationName": "Station A",
      "address": "123 Main St",
      "month": 10,
      "year": 2025,
      "totalRevenue": 1200.50,
      "totalSessions": 30
    }
  ]
}
```

---

## 6. Stations

All station endpoints require bearer token (ROLE_ADMIN) unless otherwise specified.

### GET /api/stations/overview  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách overview của tất cả trạm (nhẹ hơn so với full detail). Dùng cho FE hiển thị nhanh bảng tổng quan.

Sample `StationOverviewResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "name": "Trạm Sạc Quận 1",
      "status": "OPERATIONAL",
      "active": true
    },
    {
      "stationId": "uuid-2222-...",
      "name": "Trạm Sạc Quận 2",
      "status": "MAINTENANCE",
      "active": false
    }
  ]
}
```

### GET /api/stations?status={StationStatus}  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách trạm với thông tin cơ bản, có thể filter theo status.

Query parameters:
- `status` (optional): Filter theo trạng thái - các giá trị hợp lệ:
  - `OPERATIONAL` - Đang hoạt động
  - `MAINTENANCE` - Đang bảo trì
  - `OUT_OF_SERVICE` - Ngưng hoạt động
  - `CLOSED` - Đã đóng cửa

Sample `StationResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "name": "Trạm Sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "Công ty ABC",
      "contactPhone": "0987654321",
      "status": "OPERATIONAL",
      "active": true
    }
  ]
}
```

### GET /api/stations/detail?status={StationStatus}  
Bearer token required (ROLE_ADMIN)  
**Danh sách trạm với thông tin đầy đủ cho UI quản lý** bao gồm:
- Thông tin cơ bản của trạm
- Số lượng điểm sạc theo trạng thái (total, available, in-use, offline, maintenance)
- Doanh thu
- Phần trăm sử dụng
- Tên nhân viên phụ trách

Query parameters:
- `status` (optional): Filter theo trạng thái

Sample `StationDetailResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "name": "Trạm Sạc Quận 1",
      "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "operatorName": "Công ty ABC",
      "contactPhone": "0987654321",
      "status": "OPERATIONAL",
      "totalPoints": 10,
      "availablePoints": 5,
      "inUsePoints": 3,
      "offlinePoints": 1,
      "maintenancePoints": 1,
      "activePoints": 8,
      "revenue": 1250.75,
      "usagePercent": 30.0,
      "staffName": "Nguyễn Văn A"
    }
  ]
}
```

### POST /api/stations/create  
Bearer token required (ROLE_ADMIN)  
Tạo trạm sạc mới với số lượng điểm sạc và công suất chỉ định.  
**Lưu ý:** Trạm mới tạo sẽ có trạng thái mặc định là `OUT_OF_SERVICE` (chưa hoạt động).

Request body (`StationCreationRequest`):
```json
{
  "name": "Trạm Sạc Mới",
  "address": "456 Lê Lợi, Quận 3, TP.HCM",
  "numberOfChargingPoints": 10,
  "powerOutputKw": 50.0,
  "operatorName": "Công ty XYZ",
  "contactPhone": "0901234567"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-new-...",
    "name": "Trạm Sạc Mới",
    "address": "456 Lê Lợi, Quận 3, TP.HCM",
    "operatorName": "Công ty XYZ",
    "contactPhone": "0901234567",
    "status": "OUT_OF_SERVICE",
    "active": false
  }
}
```

### PUT /api/stations/{stationId}  
Bearer token required (ROLE_ADMIN)  
Cập nhật thông tin cơ bản của trạm (name, address, operatorName, contactPhone, status).  
**Lưu ý:** Không thay đổi số lượng charging points hoặc cấu hình phần cứng.

Request body (`StationUpdateRequest`):
```json
{
  "name": "Trạm Sạc Quận 1 - Cập Nhật",
  "address": "789 Nguyễn Thị Minh Khai, Quận 1, TP.HCM",
  "operatorName": "Công ty ABC Updated",
  "contactPhone": "0999888777",
  "status": "OPERATIONAL"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Trạm Sạc Quận 1 - Cập Nhật",
    "address": "789 Nguyễn Thị Minh Khai, Quận 1, TP.HCM",
    "operatorName": "Công ty ABC Updated",
    "contactPhone": "0999888777",
    "status": "OPERATIONAL",
    "active": true
  }
}
```

### DELETE /api/stations/{stationId}  
Bearer token required (ROLE_ADMIN)  
Xóa trạm sạc theo id. **Tất cả charging points liên quan sẽ tự động bị xóa (cascade).**

Sample response:
```json
{
  "code": 1000,
  "message": "Station deleted successfully",
  "result": null
}
```

### PATCH /api/stations/{stationId}/status?status={StationStatus}  
Bearer token required (ROLE_ADMIN)  
Cập nhật trạng thái cụ thể của trạm (truyền enum trực tiếp).

Query parameters:
- `status` (required): Trạng thái mới - `OPERATIONAL`, `MAINTENANCE`, `OUT_OF_SERVICE`, hoặc `CLOSED`

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Trạm Sạc Quận 1",
    "address": "123 Nguyễn Huệ, Quận 1, TP.HCM",
    "operatorName": "Công ty ABC",
    "contactPhone": "0987654321",
    "status": "MAINTENANCE",
    "active": false
  }
}
```

### PATCH /api/stations/{stationId}/activate  
Bearer token required (ROLE_ADMIN)  
Kích hoạt trạm: set trạng thái về `OPERATIONAL`.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Trạm Sạc Quận 1",
    "status": "OPERATIONAL",
    "active": true
  }
}
```

### PATCH /api/stations/{stationId}/deactivate  
Bearer token required (ROLE_ADMIN)  
Ngưng hoạt động trạm: set trạng thái về `OUT_OF_SERVICE`.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Trạm Sạc Quận 1",
    "status": "OUT_OF_SERVICE",
    "active": false
  }
}
```

### PATCH /api/stations/{stationId}/toggle  
Bearer token required (ROLE_ADMIN)  
Chuyển đổi trạng thái giữa `OPERATIONAL` ↔ `OUT_OF_SERVICE`.  
**Lưu ý:** Không tác động tới các trạng thái khác (MAINTENANCE, CLOSED).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Trạm Sạc Quận 1",
    "status": "OPERATIONAL",
    "active": true
  }
}
```

---

### **Station Staff Assignment Endpoints**

### GET /api/stations/{stationId}/staff  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách nhân viên đang thuộc về một trạm.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "staffId": "uuid-staff-1...",
      "fullName": "Nguyễn Văn A",
      "email": "staff1@example.com",
      "phone": "0901234567",
      "stationId": "uuid-1111-...",
      "stationName": "Trạm Sạc Quận 1"
    }
  ]
}
```

### POST /api/stations/{stationId}/staff/{staffId}  
Bearer token required (ROLE_ADMIN)  
Gán một nhân viên (staffId = userId của staff) vào trạm.  
**Business Rule:** Nếu staff đã thuộc 1 trạm khác → Error `STAFF_ALREADY_ASSIGNED` (không tự động chuyển trạm để tránh nhầm lẫn).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "staffId": "uuid-staff-1...",
    "fullName": "Nguyễn Văn A",
    "email": "staff1@example.com",
    "phone": "0901234567",
    "stationId": "uuid-1111-...",
    "stationName": "Trạm Sạc Quận 1"
  }
}
```

### DELETE /api/stations/{stationId}/staff/{staffId}  
Bearer token required (ROLE_ADMIN)  
Bỏ gán nhân viên khỏi trạm.

Sample response:
```json
{
  "code": 1000,
  "message": "Unassigned",
  "result": null
}
```

### GET /api/stations/staff/unassigned  
Bearer token required (ROLE_ADMIN)  
Danh sách nhân viên chưa được gán vào bất kỳ trạm nào.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "staffId": "uuid-staff-2...",
      "fullName": "Trần Thị B",
      "email": "staff2@example.com",
      "phone": "0907654321",
      "stationId": null,
      "stationName": null
    }
  ]
}
```

**Station Error Codes:**
- `2001`: Station Not Found
- `4001`: Staff Not Found
- `4002`: Staff Already Assigned (nhân viên đã thuộc trạm khác)
- `4003`: Staff Not In This Station (khi unassign nhân viên không thuộc trạm này)

---

## 7. Station Usage

### GET /api/stations/{stationId}/usages/realtime  
Bearer token required (ROLE_ADMIN, ROLE_STAFF)  
Lấy thông tin phiên sạc đang diễn ra tại một trạm (nếu có).

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "currentUsage": {
      "sessionId": "uuid-session-...",
      "vehicleId": "uuid-vehicle-...",
      "driverId": "uuid-driver-...",
      "startTime": "2025-09-15T10:30:00",
      "status": "IN_PROGRESS",
      "chargingPoint": {
        "id": "uuid-point-...",
        "name": "Điểm sạc 1",
        "powerKw": 50
      }
    }
  }
}
```

### GET /api/stations/{stationId}/usages/history  
Bearer token required (ROLE_ADMIN, ROLE_STAFF)  
Lịch sử các phiên sạc tại một trạm.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "sessionId": "uuid-session-...",
      "vehicleId": "uuid-vehicle-...",
      "driverId": "uuid-driver-...",
      "startTime": "2025-09-01T08:00:00",
      "endTime": "2025-09-01T09:00:00",
      "status": "COMPLETED",
      "chargingPoint": {
        "id": "uuid-point-...",
        "name": "Điểm sạc 1",
        "powerKw": 50
      },
      "revenue": 10.5
    }
  ]
}
```

### GET /api/stations/{stationId}/usages/analytics/daily  
Bearer token required (ROLE_ADMIN, ROLE_STAFF)  
Thống kê sử dụng theo ngày cho một trạm.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "date": "2025-09-01",
      "totalSessions": 10,
      "totalRevenue": 105.0,
      "totalTimeHours": 5.5
    }
  ]
}
```

### GET /api/stations/{stationId}/usages/analytics/monthly  
Bearer token required (ROLE_ADMIN, ROLE_STAFF)  
Thống kê sử dụng theo tháng cho một trạm.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "month": "2025-09",
      "totalSessions": 50,
      "totalRevenue": 525.0,
      "totalTimeHours": 30.5
    }
  ]
}
```

### GET /api/stations/{stationId}/usages/analytics/yearly  
Bearer token required (ROLE_ADMIN, ROLE_STAFF)  
Thống kê sử dụng theo năm cho một trạm.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "year": 2025,
      "totalSessions": 600,
      "totalRevenue": 6300.0,
      "totalTimeHours": 365.0
    }
  ]
}
```

---

## 8. Vehicles (Thông tin xe điện)

### POST /api/vehicles
Bearer token required (ROLE_DRIVER)  
Driver tạo xe mới cho chính mình.

Request body (`VehicleCreationRequest`):
```json
{
  "licensePlate": "30A-12345",
  "model": "Tesla Model 3",
  "batteryCapacityKwh": 75.0,
  "batteryType": "Lithium-ion"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "vehicleId": "uuid-vehicle-...",
    "licensePlate": "30A-12345",
    "model": "Tesla Model 3",
    "batteryCapacityKwh": 75.0,
    "batteryType": "Lithium-ion",
    "ownerId": "uuid-driver-..."
  }
}
```

### GET /api/vehicles/my-vehicles
Bearer token required (ROLE_DRIVER)  
Driver lấy danh sách tất cả xe của mình.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "vehicleId": "uuid-vehicle-...",
      "licensePlate": "30A-12345",
      "model": "Tesla Model 3",
      "batteryCapacityKwh": 75.0,
      "batteryType": "Lithium-ion",
      "ownerId": "uuid-driver-..."
    },
    {
      "vehicleId": "uuid-vehicle-2...",
      "licensePlate": "29B-67890",
      "model": "VinFast VF8",
      "batteryCapacityKwh": 87.7,
      "batteryType": "LFP",
      "ownerId": "uuid-driver-..."
    }
  ]
}
```

### GET /api/vehicles/my-vehicles/{vehicleId}
Bearer token required (ROLE_DRIVER)  
Driver lấy chi tiết một xe của mình.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "vehicleId": "uuid-vehicle-...",
    "licensePlate": "30A-12345",
    "model": "Tesla Model 3",
    "batteryCapacityKwh": 75.0,
    "batteryType": "Lithium-ion",
    "ownerId": "uuid-driver-..."
  }
}
```

### PUT /api/vehicles/{vehicleId}
Bearer token required (ROLE_DRIVER)  
Driver cập nhật thông tin xe của mình. Tất cả fields đều optional (partial update).

Request body (`VehicleUpdateRequest`):
```json
{
  "licensePlate": "30A-99999",
  "model": "Tesla Model 3 Long Range",
  "batteryCapacityKwh": 82.0,
  "batteryType": "Lithium-ion NCM"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "vehicleId": "uuid-vehicle-...",
    "licensePlate": "30A-99999",
    "model": "Tesla Model 3 Long Range",
    "batteryCapacityKwh": 82.0,
    "batteryType": "Lithium-ion NCM",
    "ownerId": "uuid-driver-..."
  }
}
```

### DELETE /api/vehicles/{vehicleId}
Bearer token required (ROLE_DRIVER)  
Driver xóa xe của mình.

Sample response:
```json
{
  "code": 1000,
  "message": "Vehicle deleted successfully",
  "result": null
}
```

### GET /api/vehicles/driver/{driverId}
Bearer token required (ROLE_ADMIN)  
Admin lấy danh sách xe của một driver cụ thể.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "vehicleId": "uuid-vehicle-...",
      "licensePlate": "30A-12345",
      "model": "Tesla Model 3",
      "batteryCapacityKwh": 75.0,
      "batteryType": "Lithium-ion",
      "ownerId": "uuid-driver-..."
    }
  ]
}
```

**Vehicle Validation & Business Rules:**
- Biển số xe (`licensePlate`) phải unique trong hệ thống
- Driver chỉ có thể xem/sửa/xóa xe của chính mình
- Admin có thể xem xe của bất kỳ driver nào

**Error Codes:**
- `5001`: Vehicle Not Found
- `5002`: License Plate Already Exists (biển số đã tồn tại)
- `5003`: Vehicle Does Not Belong To This Driver (xe không thuộc về driver này)

---

