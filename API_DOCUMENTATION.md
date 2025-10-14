# EV Charging Station Management System – API Reference

**Base URL:** `http://localhost:8080/evchargingstation`  
**Current Date:** October 15, 2025

All responses are wrapped in the common `ApiResponse<T>` envelope:
```json
{
  "code": 1000,
  "message": null,
  "result": { }
}
```

---

## 1. Authentication

### POST /api/auth/login
**Public** (no token required)

Login to get JWT token.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true,
    "userInfo": {
      "userId": "uuid-...",
      "email": "user@example.com",
      "phone": "0123456789",
      "dateOfBirth": "1990-01-01",
      "gender": true,
      "firstName": "John",
      "lastName": "Doe",
      "fullName": "John Doe",
      "role": "DRIVER"
    }
  }
}
```

### POST /api/auth/introspect
**Bearer token required**

Verify if a token is valid.

**Request:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

---

## 2. Users

### POST /api/users/register
**Public** (no token required)

Register a new driver account.

**Request:**
```json
{
  "email": "newdriver@example.com",
  "password": "hunter2",
  "confirmPassword": "hunter2"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-...",
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
**Bearer token required** (ROLE_DRIVER)

Get authenticated driver's profile.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-...",
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
**Bearer token required** (ROLE_DRIVER)

Update authenticated driver's profile.

**Request (all fields optional):**
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

### GET /api/users/{userId}
**Bearer token required**

Get user details by ID.

### GET /api/users
**Bearer token required** (ROLE_ADMIN)

List all drivers (admin view).

**Response:**
```json
{
  "code": 1000,
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
**Bearer token required** (ROLE_ADMIN)

Admin gets full driver information.

### PUT /api/users/driver/{driverId}
**Bearer token required** (ROLE_ADMIN)

Admin updates driver information.

### DELETE /api/users/{userId}
**Bearer token required** (ROLE_ADMIN)

Delete a user.

---

## 3. Vehicles

### POST /api/vehicles
**Bearer token required** (ROLE_DRIVER)

Register a new vehicle.

**Request:**
```json
{
  "licensePlate": "29A-12345",
  "model": "Tesla Model 3",
  "batteryCapacityKwh": 75.0,
  "batteryType": "Lithium-ion"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "vehicleId": "uuid-...",
    "licensePlate": "29A-12345",
    "model": "Tesla Model 3",
    "batteryCapacityKwh": 75.0,
    "batteryType": "Lithium-ion",
    "ownerId": "uuid-driver-..."
  }
}
```

### GET /api/vehicles/my-vehicles
**Bearer token required** (ROLE_DRIVER)

Get all vehicles of authenticated driver.

### GET /api/vehicles/my-vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Get vehicle details by ID.

### PUT /api/vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Update vehicle information.

**Request:**
```json
{
  "model": "Tesla Model 3 LR",
  "batteryCapacityKwh": 82.0,
  "batteryType": "Lithium-ion"
}
```

### DELETE /api/vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Delete a vehicle.

### GET /api/vehicles/driver/{driverId}
**Bearer token required** (ROLE_ADMIN)

Admin gets all vehicles of a specific driver.

---

## 4. Plans

### POST /api/plans
**Bearer token required** (ROLE_ADMIN)

Create a new plan.

**Request:**
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

**Billing Types:**
- `PAY_AS_YOU_GO` - monthlyFee = 0
- `MONTHLY_SUBSCRIPTION` - monthlyFee > 0
- `VIP` - monthlyFee > 0

### POST /api/plans/vip
**Bearer token required** (ROLE_ADMIN)

Create a VIP plan (billingType auto-set to VIP).

### GET /api/plans
**Public**

Get all plans.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "planId": "uuid-...",
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
**Bearer token required** (ROLE_ADMIN)

Get plan details by ID.

### PUT /api/plans/{planId}
**Bearer token required** (ROLE_ADMIN)

Update a plan.

### DELETE /api/plans/{planId}
**Bearer token required** (ROLE_ADMIN)

Delete a plan.

**Error Codes:**
- `3001`: Plan Not Found
- `3002`: Plan Name Existed
- `3003`: Invalid Plan Configuration
- `3004`: Plan In Use (cannot delete)

---

## 5. Subscriptions

### POST /api/subscriptions
**Bearer token required** (ROLE_DRIVER)

Subscribe to a plan.

**Request:**
```json
{
  "planId": "uuid-..."
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "subscriptionId": "uuid-...",
    "planName": "Premium Plan",
    "startDate": "2025-10-01",
    "endDate": "2025-11-01",
    "status": "ACTIVE",
    "autoRenew": true
  }
}
```

### GET /api/subscriptions/active
**Bearer token required** (ROLE_DRIVER)

Get active subscription.

### DELETE /api/subscriptions/{subscriptionId}
**Bearer token required** (ROLE_DRIVER)

Cancel a subscription.

### PATCH /api/subscriptions/{subscriptionId}/auto-renew?autoRenew={true|false}
**Bearer token required** (ROLE_DRIVER)

Enable/disable auto-renewal.

**Query Parameter:**
- `autoRenew`: boolean (true or false)

---

## 6. Charging Sessions

### GET /api/charging-sessions/driver/dashboard
**Bearer token required** (ROLE_DRIVER)

Get driver dashboard with statistics.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "memberSince": "2025-01-15",
    "currentPlanName": "Premium",
    "totalSessions": 25,
    "totalEnergyConsumed": 150.5,
    "totalSpent": 500.75,
    "vehicleModel": "Tesla Model 3",
    "vehiclePlate": "29A-12345"
  }
}
```

### GET /api/charging-sessions/driver/sessions
**Bearer token required** (ROLE_DRIVER)

Get all charging sessions.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "sessionId": "uuid-...",
      "startTime": "2025-10-14T08:30:00",
      "endTime": "2025-10-14T10:00:00",
      "durationMin": 90,
      "stationName": "Station A",
      "chargingPointName": "CP-01",
      "pointType": "AC",
      "startSocPercent": 20.0,
      "endSocPercent": 80.0,
      "energyKwh": 45.5,
      "costTotal": 150.0,
      "status": "COMPLETED",
      "vehicleModel": "Tesla Model 3",
      "vehiclePlate": "29A-12345"
    }
  ]
}
```

### GET /api/charging-sessions/driver/sessions/{sessionId}
**Bearer token required** (ROLE_DRIVER)

Get session details by ID.

### GET /api/charging-sessions/driver/monthly-analytics?year={year}&month={month}
**Bearer token required** (ROLE_DRIVER)

Get monthly analytics.

**Query Parameters:**
- `year` (optional): default current year
- `month` (optional): 1-12, default current month

---

## 7. Dashboard (Staff/Admin)

### GET /api/dashboard/summary?startDate={date}&endDate={date}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get dashboard summary.

**Query Parameters:**
- `startDate` (optional): YYYY-MM-DD
- `endDate` (optional): YYYY-MM-DD

### GET /api/dashboard/hourly-charging?date={date}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get hourly charging distribution.

**Query Parameter:**
- `date` (optional): YYYY-MM-DD, default today

### GET /api/dashboard/favorite-stations?limit={limit}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get top favorite stations.

**Query Parameter:**
- `limit` (optional): default 5

### GET /api/dashboard/charging-statistics?startDate={date}&endDate={date}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get charging statistics for date range.

---

## 8. Notification Settings ⭐ NEW

### GET /api/notification-settings/my-settings
**Bearer token required**

Get all notification settings for authenticated user.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "settingId": "uuid-1",
      "notificationType": "CHARGING_COMPLETE",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "uuid-2",
      "notificationType": "LOW_BATTERY",
      "channel": "SMS",
      "isEnabled": true
    },
    {
      "settingId": "uuid-3",
      "notificationType": "PROMOTIONAL",
      "channel": "EMAIL",
      "isEnabled": false
    },
    {
      "settingId": "uuid-4",
      "notificationType": "MAINTENANCE",
      "channel": "EMAIL",
      "isEnabled": true
    }
  ]
}
```

**Notification Types:**
- `CHARGING_COMPLETE` - Thông báo khi xe đã sạc đầy (Hoàn thành sạc)
- `LOW_BATTERY` - Cảnh báo khi pin dưới 20% (Pin yếu)
- `PROMOTIONAL` - Nhận thông báo về ưu đãi đặc biệt (Khuyến mãi)
- `MAINTENANCE` - Thông báo về lịch bảo trì trạm sạc (Bảo trì trạm)

**Notification Channels:**
- `EMAIL` - Gửi thông báo qua email
- `SMS` - Gửi thông báo qua tin nhắn

### PUT /api/notification-settings/my-settings
**Bearer token required**

Update multiple notification settings (batch update).  
**Use case:** When user clicks "Lưu cài đặt" button.

**Request:**
```json
{
  "settings": [
    {
      "notificationType": "CHARGING_COMPLETE",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "notificationType": "CHARGING_COMPLETE",
      "channel": "SMS",
      "isEnabled": false
    },
    {
      "notificationType": "LOW_BATTERY",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "notificationType": "LOW_BATTERY",
      "channel": "SMS",
      "isEnabled": true
    },
    {
      "notificationType": "PROMOTIONAL",
      "channel": "EMAIL",
      "isEnabled": false
    },
    {
      "notificationType": "PROMOTIONAL",
      "channel": "SMS",
      "isEnabled": false
    },
    {
      "notificationType": "MAINTENANCE",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "notificationType": "MAINTENANCE",
      "channel": "SMS",
      "isEnabled": false
    }
  ]
}
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "settingId": "uuid-1",
      "notificationType": "CHARGING_COMPLETE",
      "channel": "EMAIL",
      "isEnabled": true
    }
    // ... other settings
  ]
}
```

### PATCH /api/notification-settings/my-settings/single
**Bearer token required**

Update a single notification setting.  
**Use case:** When user toggles a single switch.

**Request:**
```json
{
  "notificationType": "LOW_BATTERY",
  "channel": "SMS",
  "isEnabled": true
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "settingId": "uuid-4",
    "notificationType": "LOW_BATTERY",
    "channel": "SMS",
    "isEnabled": true
  }
}
```

---

## 9. Overview (Admin)

### GET /api/overview
**Bearer token required** (ROLE_ADMIN)

Get system overview.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalStations": 12,
    "activeChargingPoints": 8,
    "totalDrivers": 42,
    "currentMonthRevenue": 3210.50
  }
}
```

### GET /api/overview/total-stations
**Bearer token required** (ROLE_ADMIN)

Get total number of stations.

### GET /api/overview/total-drivers
**Bearer token required** (ROLE_ADMIN)

Get total number of drivers.

---

## 10. Payment Methods

### POST /api/payment-methods
**Bearer token required** (ROLE_DRIVER)

Add a payment method.

**Request:**
```json
{
  "methodType": "CREDIT_CARD",
  "provider": "Visa",
  "token": "tok_xxxxxxxxxx"
}
```

### GET /api/payment-methods
**Bearer token required** (ROLE_DRIVER)

Get all payment methods.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "pmId": "uuid-...",
      "methodType": "CREDIT_CARD",
      "provider": "Visa",
      "lastFourDigits": "1234",
      "isDefault": true
    }
  ]
}
```

### DELETE /api/payment-methods/{pmId}
**Bearer token required** (ROLE_DRIVER)

Delete a payment method.

---

## 11. Revenue (Admin)

### GET /api/revenue/weekly?year={year}&month={month}&week={week}
**Bearer token required** (ROLE_ADMIN)

Get weekly revenue by station.

**Query Parameters:**
- `year` (optional): default current year
- `month` (optional): default current month
- `week` (optional): default current week

### GET /api/revenue/monthly?year={year}&month={month}
**Bearer token required** (ROLE_ADMIN)

Get monthly revenue by station.

### GET /api/revenue/yearly?year={year}
**Bearer token required** (ROLE_ADMIN)

Get yearly revenue by station.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
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

## 12. Stations

### POST /api/stations
**Bearer token required** (ROLE_ADMIN)

Create a charging station.

**Request:**
```json
{
  "name": "Station A",
  "address": "123 Main Street",
  "latitude": 21.0285,
  "longitude": 105.8542
}
```

### GET /api/stations
**Public**

Get all stations.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "name": "Station A",
      "address": "123 Main Street",
      "latitude": 21.0285,
      "longitude": 105.8542,
      "totalChargingPoints": 5,
      "availablePoints": 3,
      "status": "OPERATIONAL"
    }
  ]
}
```

### GET /api/stations/{stationId}
**Public**

Get station details.

### PUT /api/stations/{stationId}
**Bearer token required** (ROLE_ADMIN)

Update a station.

### DELETE /api/stations/{stationId}
**Bearer token required** (ROLE_ADMIN)

Delete a station.

---

## 13. Station Usage (Staff/Admin)

### GET /api/station-usage/{stationId}/today
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get station usage for today.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "stationName": "Station A",
    "address": "123 Main St",
    "date": "2025-10-15",
    "totalChargingPoints": 5,
    "currentInUsePoints": 2,
    "currentAvailablePoints": 3,
    "currentUsagePercent": 40.0,
    "totalSessionsToday": 15,
    "completedSessionsToday": 12,
    "activeSessionsToday": 2,
    "totalEnergyToday": 125.5,
    "totalRevenueToday": 450.0,
    "peakHour": 14,
    "peakUsagePercent": 80.0
  }
}
```

### GET /api/station-usage/{stationId}/date?date={date}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get station usage for specific date.

**Query Parameter:**
- `date` (required): YYYY-MM-DD

### GET /api/station-usage/all/today
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get usage for all stations today.

### GET /api/station-usage/all/date?date={date}
**Bearer token required** (ROLE_STAFF or ROLE_ADMIN)

Get usage for all stations on specific date.

---

## Common Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request successful |
| 1001 | Uncategorized error | Unknown error |
| 1002 | Invalid key | Invalid request parameter |
| 1003 | User existed | Email already registered |
| 1004 | User not existed | User not found |
| 1005 | User not found | User not found |
| 1006 | Unauthenticated | Not logged in or invalid token |
| 1007 | Unauthorized | No permission to access |
| 1008 | Invalid DOB | Date of birth is invalid |
| 2001 | License plate existed | License plate already registered |
| 2002 | Vehicle not found | Vehicle not found |
| 3001 | Plan not found | Plan not found |
| 3002 | Plan name existed | Plan name already exists |
| 3003 | Invalid plan config | Plan configuration violates rules |
| 3004 | Plan in use | Cannot delete plan that is in use |
| 4001 | Station not found | Station not found |
| 5001 | Subscription not found | Subscription not found |

---

## Authentication

Most endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

To obtain a token, use `/api/auth/login` endpoint.

---

## Notes

- All datetime fields are in ISO 8601 format: `2025-10-15T10:30:00`
- All date fields are in format: `YYYY-MM-DD`
- Monetary values are in VND (Vietnamese Dong)
- Energy values are in kWh (kilowatt-hours)
- Percentage values range: 0.0 - 100.0
- Base URL: `http://localhost:8080/evchargingstation`

---

## Frontend Integration Tips

### Notification Settings UI Mapping

Based on the UI screenshot provided:

| UI Label | Backend Enum | Channels Available |
|----------|-------------|-------------------|
| **Hoàn thành sạc** (Thông báo khi xe đã sạc đầy) | `CHARGING_COMPLETE` | EMAIL, SMS |
| **Pin yếu** (Cảnh báo khi pin dưới 20%) | `LOW_BATTERY` | EMAIL, SMS |
| **Khuyến mãi** (Nhận thông báo về ưu đãi đặc biệt) | `PROMOTIONAL` | EMAIL, SMS |
| **Bảo trì trạm** (Thông báo về lịch bảo trì trạm sạc) | `MAINTENANCE` | EMAIL, SMS |

**Total: 4 notification types × 2 channels = 8 toggle switches**

**Implementation:**
- Each toggle switch → Call `PATCH /api/notification-settings/my-settings/single`
- "Lưu cài đặt" button → Call `PUT /api/notification-settings/my-settings` with all 8 settings

---

**Last Updated:** October 15, 2025
