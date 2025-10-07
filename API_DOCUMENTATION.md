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
Get the authenticated driver's profile.

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
    "role": "DRIVER"
  }
}
```

### PATCH /api/users/driver/myInfo  
Bearer token required (ROLE_DRIVER)  
Partially update the authenticated driver's profile. Same response shape as above.

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

All station endpoints require bearer token (ROLE_ADMIN).

### GET /api/stations/overview  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách overview của tất cả trạm (nhẹ hơn so với full detail).

Sample `StationOverviewResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "name": "Station A",
      "status": "OPERATIONAL",
      "active": true
    }
  ]
}
```

### GET /api/stations?status={StationStatus}  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách trạm với thông tin cơ bản, có thể filter theo status.

Query parameters:
- `status` (optional): Filter theo trạng thái (OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE, UNDER_CONSTRUCTION)

Sample `StationResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "name": "Station A",
      "address": "123 Main St",
      "operatorName": "Operator X",
      "contactPhone": "0987654321",
      "status": "OPERATIONAL",
      "active": true
    }
  ]
}
```

### POST /api/stations/create  
Bearer token required (ROLE_ADMIN)  
Tạo trạm sạc mới.

Request body (`StationCreationRequest`):
```json
{
  "name": "New Station",
  "address": "456 New St",
  "operatorName": "Operator Y",
  "contactPhone": "0987654321",
  "status": "OPERATIONAL"
}
```

### PATCH /api/stations/{stationId}/status?status={StationStatus}  
Bearer token required (ROLE_ADMIN)  
Cập nhật trạng thái cụ thể của trạm.

### PATCH /api/stations/{stationId}/activate  
Bearer token required (ROLE_ADMIN)  
Đặt trạng thái hoạt động (OPERATIONAL).

### PATCH /api/stations/{stationId}/deactivate  
Bearer token required (ROLE_ADMIN)  
Đặt trạng thái ngưng hoạt động (OUT_OF_SERVICE).

### PATCH /api/stations/{stationId}/toggle  
Bearer token required (ROLE_ADMIN)  
Toggle giữa OPERATIONAL và OUT_OF_SERVICE.

Each status update returns a single `StationResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "Station A",
    "address": "123 Main St",
    "operatorName": "Operator X",
    "contactPhone": "0987654321",
    "status": "OUT_OF_SERVICE",
    "active": false
  }
}
```

### Staff Assignment Endpoints

### GET /api/stations/{stationId}/staff  
Bearer token required (ROLE_ADMIN)  
Lấy danh sách nhân viên đang thuộc về một trạm.

### POST /api/stations/{stationId}/staff/{staffId}  
Bearer token required (ROLE_ADMIN)  
Gán một nhân viên vào trạm.

### DELETE /api/stations/{stationId}/staff/{staffId}  
Bearer token required (ROLE_ADMIN)  
Bỏ gán nhân viên khỏi trạm.

Sample `StaffSummaryResponse`:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "staffId": "uuid-2222-...",
      "employeeNo": "EMP001",
      "position": "Technician",
      "email": "tech@example.com",
      "fullName": "Bob Tran",
      "stationId": "uuid-1111-...",
      "stationName": "Station A"
    }
  ]
}
```

### GET /api/stations/staff/unassigned  
Bearer token required (ROLE_ADMIN)  
Danh sách nhân viên chưa được gán vào bất kỳ trạm nào. Same `StaffSummaryResponse` shape.

---

## 7. Station Usage

Endpoints để theo dõi mức độ sử dụng trạm sạc.

### GET /api/station-usage/{stationId}/today  
Bearer token required (ROLE_ADMIN or ROLE_STAFF)  
Lấy mức độ sử dụng của MỘT trạm trong ngày hôm nay.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "stationName": "Station A",
    "date": "2025-10-07",
    "totalChargingPoints": 10,
    "occupiedPoints": 7,
    "availablePoints": 3,
    "usagePercent": 70.0,
    "totalSessions": 15,
    "totalEnergyDelivered": 250.5
  }
}
```

### GET /api/station-usage/{stationId}?date={date}  
Bearer token required (ROLE_ADMIN or ROLE_STAFF)  
Lấy mức độ sử dụng của MỘT trạm theo ngày cụ thể.

Query parameters:
- `date` (optional): Ngày cần xem (format: yyyy-MM-dd), mặc định là hôm nay

### GET /api/station-usage/all/today  
Bearer token required (ROLE_ADMIN)  
Lấy mức độ sử dụng của TẤT CẢ trạm trong ngày hôm nay.

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "stationId": "uuid-1111-...",
      "stationName": "Station A",
      "date": "2025-10-07",
      "totalChargingPoints": 10,
      "occupiedPoints": 7,
      "availablePoints": 3,
      "usagePercent": 70.0,
      "totalSessions": 15,
      "totalEnergyDelivered": 250.5
    },
    {
      "stationId": "uuid-2222-...",
      "stationName": "Station B",
      "date": "2025-10-07",
      "totalChargingPoints": 8,
      "occupiedPoints": 5,
      "availablePoints": 3,
      "usagePercent": 62.5,
      "totalSessions": 12,
      "totalEnergyDelivered": 180.3
    }
  ]
}
```

### GET /api/station-usage/all?date={date}  
Bearer token required (ROLE_ADMIN)  
Lấy mức độ sử dụng của TẤT CẢ trạm theo ngày cụ thể.

Query parameters:
- `date` (optional): Ngày cần xem (format: yyyy-MM-dd), mặc định là hôm nay

---

**Notes on Authentication**  
- Only `/api/auth/login`, `/api/users/register`, and `/api/plans` (GET) are publicly accessible.  
- All other endpoints require a valid Bearer JWT in `Authorization: Bearer <token>`.  
- Sensitive operations (overview, plan management, revenue, station management, drivers listing/deletion) are restricted to **ROLE_ADMIN**.
- Station usage endpoints for individual stations can be accessed by both **ROLE_ADMIN** and **ROLE_STAFF**.
- Station usage endpoints for all stations are restricted to **ROLE_ADMIN** only.
