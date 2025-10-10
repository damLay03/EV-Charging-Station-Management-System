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
  "numberOfChargingPoints": 10,
  "powerOutputKw": 50.0,
  "operatorName": "Operator Y",
  "contactPhone": "0987654321"
}
```

Sample response:
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "stationId": "uuid-1111-...",
    "name": "New Station",
    "address": "456 New St",
    "operatorName": "Operator Y",
    "contactPhone": "0987654321",
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
  "name": "Updated Station Name",
  "address": "789 Updated St",
  "operatorName": "New Operator",
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
    "name": "Updated Station Name",
    "address": "789 Updated St",
    "operatorName": "New Operator",
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
Cập nhật trạng thái của trạm.
