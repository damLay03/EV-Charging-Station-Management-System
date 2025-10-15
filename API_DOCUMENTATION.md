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

## Table of Contents

1. [Authentication](#1-authentication)
2. [Users](#2-users)
3. [Vehicles](#3-vehicles)
4. [Plans](#4-plans)
5. [Subscriptions](#5-subscriptions)
6. [Payment Methods](#6-payment-methods)
7. [Charging Sessions](#7-charging-sessions)
8. [Dashboard (Driver)](#8-dashboard-driver)
9. [Notification Settings](#9-notification-settings)
10. [Overview (Admin)](#10-overview-admin)
11. [Stations](#11-stations)
12. [Station Usage](#12-station-usage)
13. [Revenue](#13-revenue)
14. [Error Codes](#error-codes)
15. [Authentication](#authentication-header)

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

---

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

**Validation:**
- `email`: Not blank, valid email format
- `password`: Not blank, min 6 characters
- `confirmPassword`: Must match password

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

---

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

---

### PATCH /api/users/driver/myInfo
**Bearer token required** (ROLE_DRIVER)

Update authenticated driver's profile. Driver can only update their own profile.

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

**Response:**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-...",
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

---

### GET /api/users/{userId}
**Bearer token required**

Get user details by ID.

**Response:**
```json
{
  "code": 1000,
  "result": {
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
```

---

### GET /api/users
**Bearer token required** (ROLE_ADMIN)

List all drivers (admin view with session counts and spending).

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

---

### GET /api/users/driver/{driverId}/info
**Bearer token required** (ROLE_ADMIN)

Admin gets full driver information including address and joinDate.

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

---

### PUT /api/users/driver/{driverId}
**Bearer token required** (ROLE_ADMIN)

Admin updates driver information. Cannot update email, password, or joinDate.

**Request:**
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

**Response:**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-...",
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

---

### DELETE /api/users/{userId}
**Bearer token required** (ROLE_ADMIN)

Delete a user (hard delete).

**Response:**
```json
{
  "code": 1000,
  "message": "Deleted"
}
```

---

## 3. Vehicles

### POST /api/vehicles
**Bearer token required** (ROLE_DRIVER)

Register a new vehicle for the authenticated driver.

**Request:**
```json
{
  "licensePlate": "29A-12345",
  "model": "Tesla Model 3",
  "batteryCapacityKwh": 75.0,
  "batteryType": "Lithium-ion"
}
```

**Validation:**
- `licensePlate`: Not blank
- `model`: Not blank
- `batteryCapacityKwh`: Must be positive

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

---

### GET /api/vehicles/my-vehicles
**Bearer token required** (ROLE_DRIVER)

Get all vehicles of authenticated driver.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "vehicleId": "uuid-...",
      "licensePlate": "29A-12345",
      "model": "Tesla Model 3",
      "batteryCapacityKwh": 75.0,
      "batteryType": "Lithium-ion",
      "ownerId": "uuid-driver-..."
    }
  ]
}
```

---

### GET /api/vehicles/my-vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Get vehicle details by ID. Driver can only access their own vehicles.

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

---

### PUT /api/vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Update vehicle information. Driver can only update their own vehicles.

**Request:**
```json
{
  "model": "Tesla Model 3 LR",
  "batteryCapacityKwh": 82.0,
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
    "model": "Tesla Model 3 LR",
    "batteryCapacityKwh": 82.0,
    "batteryType": "Lithium-ion",
    "ownerId": "uuid-driver-..."
  }
}
```

---

### DELETE /api/vehicles/{vehicleId}
**Bearer token required** (ROLE_DRIVER)

Delete a vehicle. Driver can only delete their own vehicles.

**Response:**
```json
{
  "code": 1000,
  "message": "Vehicle deleted successfully"
}
```

---

### GET /api/vehicles/driver/{driverId}
**Bearer token required** (ROLE_ADMIN)

Admin gets all vehicles of a specific driver.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "vehicleId": "uuid-...",
      "licensePlate": "29A-12345",
      "model": "Tesla Model 3",
      "batteryCapacityKwh": 75.0,
      "batteryType": "Lithium-ion",
      "ownerId": "uuid-driver-..."
    }
  ]
}
```

---

## 4. Plans

### POST /api/plans
**Bearer token required** (ROLE_ADMIN)

Create a new plan. Billing type determines validation rules.

**Request:**
```json
{
  "name": "Premium Plan",
  "billingType": "MONTHLY_SUBSCRIPTION",
  "pricePerKwh": 3500.0,
  "pricePerMinute": 500.0,
  "monthlyFee": 99000.0,
  "benefits": "Ưu đãi giảm 20%, hỗ trợ 24/7"
}
```

**Billing Types & Rules:**
- `PAY_AS_YOU_GO`: monthlyFee = 0, pricePerKwh ≥ 0, pricePerMinute ≥ 0
- `MONTHLY_SUBSCRIPTION`: monthlyFee > 0, pricePerKwh ≥ 0, pricePerMinute ≥ 0
- `VIP`: monthlyFee > 0, pricePerKwh ≥ 0, pricePerMinute ≥ 0

**Validation:**
- `name`: Not blank, unique
- `billingType`: Required
- `pricePerKwh`: ≥ 0
- `pricePerMinute`: ≥ 0
- `monthlyFee`: ≥ 0
- `benefits`: Max 1000 characters

**Response:**
```json
{
  "code": 1000,
  "result": {
    "planId": "uuid-...",
    "name": "Premium Plan",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 3500.0,
    "pricePerMinute": 500.0,
    "monthlyFee": 99000.0,
    "benefits": "Ưu đãi giảm 20%, hỗ trợ 24/7"
  }
}
```

**Error Codes:**
- `3002`: Plan name already exists
- `3003`: Invalid plan configuration (violates billing type rules)

---

### GET /api/plans
**Bearer token required** (ROLE_ADMIN)

Get all plans (unpaginated).

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "planId": "uuid-...",
      "name": "Basic Plan",
      "billingType": "PAY_AS_YOU_GO",
      "pricePerKwh": 4000.0,
      "pricePerMinute": 600.0,
      "monthlyFee": 0.0,
      "benefits": "Pay as you go"
    },
    {
      "planId": "uuid-...",
      "name": "Premium Plan",
      "billingType": "MONTHLY_SUBSCRIPTION",
      "pricePerKwh": 3500.0,
      "pricePerMinute": 500.0,
      "monthlyFee": 99000.0,
      "benefits": "Ưu đãi giảm 20%, hỗ trợ 24/7"
    },
    {
      "planId": "uuid-...",
      "name": "VIP Plan",
      "billingType": "VIP",
      "pricePerKwh": 3000.0,
      "pricePerMinute": 400.0,
      "monthlyFee": 199000.0,
      "benefits": "Ưu đãi tối đa, hỗ trợ VIP 24/7, ưu tiên đặt chỗ"
    }
  ]
}
```

---

### GET /api/plans/{planId}
**Bearer token required** (ROLE_ADMIN)

Get plan details by ID.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "planId": "uuid-...",
    "name": "Premium Plan",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 3500.0,
    "pricePerMinute": 500.0,
    "monthlyFee": 99000.0,
    "benefits": "Ưu đãi giảm 20%, hỗ trợ 24/7"
  }
}
```

**Error Codes:**
- `3001`: Plan not found

---

### PUT /api/plans/{planId}
**Bearer token required** (ROLE_ADMIN)

Update a plan. Same validation rules apply as creation.

**Request:**
```json
{
  "name": "Premium Plan Updated",
  "billingType": "MONTHLY_SUBSCRIPTION",
  "pricePerKwh": 3400.0,
  "pricePerMinute": 480.0,
  "monthlyFee": 95000.0,
  "benefits": "Ưu đãi giảm 25%, hỗ trợ 24/7"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "planId": "uuid-...",
    "name": "Premium Plan Updated",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 3400.0,
    "pricePerMinute": 480.0,
    "monthlyFee": 95000.0,
    "benefits": "Ưu đãi giảm 25%, hỗ trợ 24/7"
  }
}
```

**Error Codes:**
- `3001`: Plan not found
- `3002`: Plan name already exists (if changing to duplicate name)
- `3003`: Invalid plan configuration

---

### DELETE /api/plans/{planId}
**Bearer token required** (ROLE_ADMIN)

Delete a plan. Cannot delete if plan is in use by active subscriptions.

**Response:**
```json
{
  "code": 1000,
  "message": "Plan deleted successfully"
}
```

**Error Codes:**
- `3001`: Plan not found
- `3004`: Plan in use (cannot delete)

---

## 5. Subscriptions

### POST /api/subscriptions
**Bearer token required** (ROLE_DRIVER)

Subscribe to a plan. If plan has monthly fee, must provide payment method.

**Request:**
```json
{
  "planId": "uuid-...",
  "paymentMethodId": "uuid-..."
}
```

**Notes:**
- `paymentMethodId` is optional for PAY_AS_YOU_GO plans (monthlyFee = 0)
- `paymentMethodId` is required for MONTHLY_SUBSCRIPTION and VIP plans

**Response:**
```json
{
  "code": 1000,
  "result": {
    "subscriptionId": "uuid-...",
    "planName": "Premium Plan",
    "startDate": "2025-10-15",
    "endDate": "2025-11-15",
    "status": "ACTIVE",
    "autoRenew": true
  }
}
```

---

### GET /api/subscriptions/active
**Bearer token required** (ROLE_DRIVER)

Get active subscription of authenticated driver.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "subscriptionId": "uuid-...",
    "planName": "Premium Plan",
    "startDate": "2025-10-15",
    "endDate": "2025-11-15",
    "status": "ACTIVE",
    "autoRenew": true
  }
}
```

**Note:** Returns `null` in result if no active subscription.

---

### DELETE /api/subscriptions/{subscriptionId}
**Bearer token required** (ROLE_DRIVER)

Cancel a subscription. Driver can only cancel their own subscriptions.

**Response:**
```json
{
  "code": 1000,
  "message": "Subscription cancelled successfully"
}
```

**Error Codes:**
- `5001`: Subscription not found

---

### PATCH /api/subscriptions/{subscriptionId}/auto-renew?autoRenew={true|false}
**Bearer token required** (ROLE_DRIVER)

Enable or disable auto-renewal for a subscription.

**Query Parameter:**
- `autoRenew`: boolean (true or false)

**Example:** `PATCH /api/subscriptions/{subscriptionId}/auto-renew?autoRenew=true`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "subscriptionId": "uuid-...",
    "planName": "Premium Plan",
    "startDate": "2025-10-15",
    "endDate": "2025-11-15",
    "status": "ACTIVE",
    "autoRenew": true
  }
}
```

**Error Codes:**
- `5001`: Subscription not found

---

## 6. Payment Methods

### POST /api/payment-methods
**Bearer token required** (ROLE_DRIVER)

Add a payment method for the authenticated driver.

**Request:**
```json
{
  "methodType": "CREDIT_CARD",
  "provider": "Visa",
  "token": "tok_xxxxxxxxxx"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "pmId": "uuid-...",
    "methodType": "CREDIT_CARD",
    "provider": "Visa",
    "lastFourDigits": "1234",
    "isDefault": true
  }
}
```

---

### GET /api/payment-methods
**Bearer token required** (ROLE_DRIVER)

Get all payment methods of authenticated driver.

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
    },
    {
      "pmId": "uuid-...",
      "methodType": "E_WALLET",
      "provider": "MoMo",
      "lastFourDigits": "5678",
      "isDefault": false
    }
  ]
}
```

---

### DELETE /api/payment-methods/{pmId}
**Bearer token required** (ROLE_DRIVER)

Delete a payment method. Driver can only delete their own payment methods.

**Response:**
```json
{
  "code": 1000,
  "message": "Payment method deleted successfully"
}
```

---

## 7. Charging Sessions

### GET /api/charging-sessions/my-dashboard
**Bearer token required** (ROLE_DRIVER)

Get driver dashboard overview including total costs, energy, sessions, and vehicle info.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalCost": 727690.0,
    "totalEnergyKwh": 212.9,
    "totalSessions": 5,
    "averageCostPerMonth": "3418đ",
    "vehicleModel": "Tesla Model 3",
    "licensePlate": "30A-12345",
    "currentBatterySoc": 75
  }
}
```

**Note:** 
- `totalCost`: Total spending on charging sessions
- `totalEnergyKwh`: Total energy consumed across all sessions
- `totalSessions`: Number of completed charging sessions
- `averageCostPerMonth`: Average monthly cost (formatted string)
- `currentBatterySoc`: Current battery state of charge (0-100%)

---

### GET /api/charging-sessions/my-sessions
**Bearer token required** (ROLE_DRIVER)

Get charging session history of authenticated driver, sorted by start time descending (newest first).

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
      "costTotal": 150000.0,
      "status": "COMPLETED",
      "vehicleModel": "Tesla Model 3",
      "vehiclePlate": "29A-12345"
    }
  ]
}
```

---

### GET /api/charging-sessions/{sessionId}
**Bearer token required** (ROLE_DRIVER)

Get session details by ID. Driver can only access their own sessions.

**Response:**
```json
{
  "code": 1000,
  "result": {
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
    "costTotal": 150000.0,
    "status": "COMPLETED",
    "vehicleModel": "Tesla Model 3",
    "vehiclePlate": "29A-12345"
  }
}
```

---

### GET /api/charging-sessions/my-analytics/monthly
**Bearer token required** (ROLE_DRIVER)

Get monthly analytics for the last 5 months. Used for analytics charts showing cost, energy, and session trends.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "month": 10,
      "year": 2025,
      "totalCost": 450000.0,
      "totalEnergyKwh": 120.5,
      "totalSessions": 8
    },
    {
      "month": 9,
      "year": 2025,
      "totalCost": 380000.0,
      "totalEnergyKwh": 105.3,
      "totalSessions": 6
    }
  ]
}
```

**Note:** Returns data for 5 most recent months including current month.

---

## 8. Dashboard (Driver)

### GET /api/dashboard/summary?period={period}
**Bearer token required** (ROLE_DRIVER)

Get dashboard summary statistics for the authenticated driver.

**Query Parameters:**
- `period` (optional): "today", "week", "month" (default: "month")

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalRevenue": 727690.0,
    "totalEnergyUsed": 212.9,
    "totalSessions": 5,
    "averagePricePerKwh": 3418.5
  }
}
```

**Note:**
- `totalRevenue`: Total cost spent in the period
- `totalEnergyUsed`: Total kWh consumed
- `totalSessions`: Number of sessions
- `averagePricePerKwh`: Average price per kWh

---

### GET /api/dashboard/hourly-sessions?date={date}
**Bearer token required** (ROLE_DRIVER)

Get hourly charging sessions distribution for a specific date. Used for hourly usage charts.

**Query Parameters:**
- `date` (optional): YYYY-MM-DD format (default: today)

**Example:** `GET /api/dashboard/hourly-sessions?date=2025-10-15`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "hour": 8,
      "sessionCount": 3,
      "totalEnergyKwh": 45.5
    },
    {
      "hour": 14,
      "sessionCount": 5,
      "totalEnergyKwh": 67.8
    }
  ]
}
```

---

### GET /api/dashboard/favorite-stations?limit={limit}
**Bearer token required** (ROLE_DRIVER)

Get top favorite stations (most frequently used by the driver).

**Query Parameters:**
- `limit` (optional): Number of stations to return (default: 5)

**Example:** `GET /api/dashboard/favorite-stations?limit=5`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "stationName": "Station A",
      "address": "123 Main Street",
      "visitCount": 15,
      "totalEnergyKwh": 125.5,
      "totalSpent": 450000.0
    }
  ]
}
```

---

### GET /api/dashboard/charging-statistics
**Bearer token required** (ROLE_DRIVER)

Get charging habit statistics for the authenticated driver.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "averageSessionDuration": 85,
    "averageEnergyPerSession": 42.5,
    "mostFrequentHour": 14,
    "mostUsedStation": "Station A",
    "preferredChargingDay": "Monday"
  }
}
```

---

## 9. Notification Settings

### GET /api/notification
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
      "notificationType": "CHARGING_COMPLETE",
      "channel": "SMS",
      "isEnabled": false
    },
    {
      "settingId": "uuid-3",
      "notificationType": "LOW_BATTERY",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "uuid-4",
      "notificationType": "LOW_BATTERY",
      "channel": "SMS",
      "isEnabled": true
    },
    {
      "settingId": "uuid-5",
      "notificationType": "PROMOTIONAL",
      "channel": "EMAIL",
      "isEnabled": false
    },
    {
      "settingId": "uuid-6",
      "notificationType": "PROMOTIONAL",
      "channel": "SMS",
      "isEnabled": false
    },
    {
      "settingId": "uuid-7",
      "notificationType": "MAINTENANCE",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "uuid-8",
      "notificationType": "MAINTENANCE",
      "channel": "SMS",
      "isEnabled": false
    }
  ]
}
```

**Notification Types:**
- `CHARGING_COMPLETE` - Thông báo khi xe đã sạc đầy
- `LOW_BATTERY` - Cảnh báo khi pin dưới 20%
- `PROMOTIONAL` - Nhận thông báo về ưu đãi đặc biệt
- `MAINTENANCE` - Thông báo về lịch bảo trì trạm sạc

**Notification Channels:**
- `EMAIL` - Gửi thông báo qua email
- `SMS` - Gửi thông báo qua tin nhắn

**Note:** Total of 4 notification types × 2 channels = 8 settings per user

---

### PUT /api/notification
**Bearer token required**

Update multiple notification settings (batch update). Use when user clicks "Lưu cài đặt" button.

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

**Validation:**
- `settings`: Not empty
- Each setting must have: `notificationType`, `channel`, `isEnabled`

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
    // ... all 8 settings updated
  ]
}
```

---

### PATCH /api/notification/single
**Bearer token required**

Update a single notification setting. Use when user toggles a single switch.

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

## 10. Overview (Admin)

### GET /api/overview
**Bearer token required** (ROLE_ADMIN)

Get system overview statistics.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalStations": 12,
    "activeChargingPoints": 45,
    "totalDrivers": 387,
    "currentMonthRevenue": 12450000.0
  }
}
```

**Note:**
- `totalStations`: Total number of stations in the system
- `activeChargingPoints`: Number of charging points currently AVAILABLE or OCCUPIED
- `totalDrivers`: Total number of registered drivers
- `currentMonthRevenue`: Revenue for the current month

---

## 11. Stations

### GET /api/stations/overview
**Bearer token required** (ROLE_ADMIN)

Get station overview list (lightweight response for listing).

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "name": "Station A",
      "address": "123 Main Street",
      "status": "OPERATIONAL",
      "active": true
    },
    {
      "stationId": "uuid-...",
      "name": "Station B",
      "address": "456 Second Street",
      "status": "MAINTENANCE",
      "active": false
    }
  ]
}
```

**Note:** 
- `active`: true if status is OPERATIONAL, false otherwise

---

### GET /api/stations?status={status}
**Bearer token required** (ROLE_ADMIN)

Get stations with basic information, optionally filtered by status.

**Query Parameters:**
- `status` (optional): OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE, CLOSED

**Example:** `GET /api/stations?status=OPERATIONAL`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "name": "Station A",
      "address": "123 Main Street",
      "operatorName": "ABC Operator",
      "contactPhone": "0123456789",
      "status": "OPERATIONAL",
      "totalChargingPoints": 5,
      "chargingPoints": [
        {
          "pointId": "uuid-...",
          "maxPowerKw": 150.0,
          "status": "AVAILABLE"
        }
      ]
    }
  ]
}
```

**Station Status Values:**
- `OPERATIONAL` - Hoạt động bình thường
- `MAINTENANCE` - Đang bảo trì
- `OUT_OF_SERVICE` - Ngưng hoạt động
- `CLOSED` - Đã đóng cửa

---

### GET /api/stations/detail?status={status}
**Bearer token required** (ROLE_ADMIN)

Get stations with full detail including charging points, revenue, usage percentage, and staff.

**Query Parameters:**
- `status` (optional): Filter by station status

**Example:** `GET /api/stations/detail?status=OPERATIONAL`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "name": "Station A",
      "address": "123 Main Street",
      "status": "OPERATIONAL",
      "totalChargingPoints": 10,
      "activeChargingPoints": 8,
      "offlineChargingPoints": 1,
      "maintenanceChargingPoints": 1,
      "chargingPointsSummary": "Tổng: 10 | Hoạt động: 8 | Offline: 1 | Bảo trì: 1",
      "revenue": 2450000.0,
      "usagePercent": 40.0,
      "staffName": "Nguyễn Văn A"
    }
  ]
}
```

**Note:**
- `totalChargingPoints`: Total number of charging points
- `activeChargingPoints`: AVAILABLE + OCCUPIED points
- `offlineChargingPoints`: OUT_OF_SERVICE points
- `maintenanceChargingPoints`: MAINTENANCE points
- `revenue`: Total revenue from all sessions at this station
- `usagePercent`: (OCCUPIED / total) × 100
- `staffName`: Name of assigned staff (null if none)
- Fields may be `null` if no data available (doesn't fabricate data)

---

### POST /api/stations/git
**Bearer token required** (ROLE_ADMIN)

Create a new charging station with specified number of charging points.

**Request:**
```json
{
  "name": "New Station",
  "address": "789 Third Street",
  "numberOfChargingPoints": 8,
  "powerOutputKw": 150.0,
  "operatorName": "ABC Operator",
  "contactPhone": "0123456789"
}
```

**Validation:**
- `name`: Not blank
- `address`: Not blank
- `numberOfChargingPoints`: Min 1
- `powerOutputKw`: Min 1

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "New Station",
    "address": "789 Third Street",
    "operatorName": "ABC Operator",
    "contactPhone": "0123456789",
    "status": "OUT_OF_SERVICE",
    "totalChargingPoints": 8,
    "chargingPoints": [
      {
        "pointId": "uuid-...",
        "maxPowerKw": 150.0,
        "status": "AVAILABLE"
      }
    ]
  }
}
```

**Note:** New stations are created with status OUT_OF_SERVICE. Admin must activate them.

---

### PUT /api/stations/{stationId}
**Bearer token required** (ROLE_ADMIN)

Update station information (name, address, operator, contact, status).

**Request:**
```json
{
  "name": "Updated Station Name",
  "address": "789 Third Street Updated",
  "operatorName": "XYZ Operator",
  "contactPhone": "0987654321",
  "status": "OPERATIONAL"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "Updated Station Name",
    "address": "789 Third Street Updated",
    "operatorName": "XYZ Operator",
    "contactPhone": "0987654321",
    "status": "OPERATIONAL",
    "totalChargingPoints": 8,
    "chargingPoints": [...]
  }
}
```

**Error Codes:**
- `4001`: Station not found

---

### DELETE /api/stations/{stationId}
**Bearer token required** (ROLE_ADMIN)

Delete a station. All charging points will be deleted (cascade).

**Response:**
```json
{
  "code": 1000,
  "message": "Station deleted successfully"
}
```

**Error Codes:**
- `4001`: Station not found

---

### PATCH /api/stations/{stationId}/status?status={status}
**Bearer token required** (ROLE_ADMIN)

Update station status to any valid enum value.

**Query Parameters:**
- `status` (required): OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE, CLOSED

**Example:** `PATCH /api/stations/{stationId}/status?status=OPERATIONAL`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "Station A",
    "address": "123 Main Street",
    "status": "OPERATIONAL",
    "totalChargingPoints": 5,
    "chargingPoints": [...]
  }
}
```

---

### PATCH /api/stations/{stationId}/activate
**Bearer token required** (ROLE_ADMIN)

Set station status to OPERATIONAL.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "Station A",
    "status": "OPERATIONAL",
    "...": "..."
  }
}
```

---

### PATCH /api/stations/{stationId}/deactivate
**Bearer token required** (ROLE_ADMIN)

Set station status to OUT_OF_SERVICE.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "Station A",
    "status": "OUT_OF_SERVICE",
    "...": "..."
  }
}
```

---

### PATCH /api/stations/{stationId}/toggle
**Bearer token required** (ROLE_ADMIN)

Toggle station status between OPERATIONAL ↔ OUT_OF_SERVICE.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "name": "Station A",
    "status": "OPERATIONAL",
    "...": "..."
  }
}
```

**Note:** Only toggles between OPERATIONAL and OUT_OF_SERVICE. Does not affect MAINTENANCE or CLOSED status.

---

### GET /api/stations/{stationId}/staff
**Bearer token required** (ROLE_ADMIN)

Get list of staff assigned to a station.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "staffId": "uuid-...",
      "fullName": "Nguyễn Văn A",
      "email": "staff1@example.com",
      "phone": "0123456789",
      "stationId": "uuid-...",
      "stationName": "Station A"
    }
  ]
}
```

---

### POST /api/stations/{stationId}/staff/{staffId}
**Bearer token required** (ROLE_ADMIN)

Assign a staff member to a station.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "staffId": "uuid-...",
    "fullName": "Nguyễn Văn A",
    "email": "staff1@example.com",
    "phone": "0123456789",
    "stationId": "uuid-...",
    "stationName": "Station A"
  }
}
```

**Error Codes:**
- `4001`: Station not found
- Staff already assigned to a station (custom error)

---

### DELETE /api/stations/{stationId}/staff/{staffId}
**Bearer token required** (ROLE_ADMIN)

Unassign a staff member from a station.

**Response:**
```json
{
  "code": 1000,
  "message": "Unassigned"
}
```

**Error Codes:**
- `4001`: Station not found
- Staff not in station (custom error)

---

### GET /api/stations/staff/unassigned
**Bearer token required** (ROLE_ADMIN)

Get list of staff not assigned to any station.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "staffId": "uuid-...",
      "fullName": "Trần Thị B",
      "email": "staff2@example.com",
      "phone": "0987654321",
      "stationId": null,
      "stationName": null
    }
  ]
}
```

---

## 12. Station Usage

### GET /api/station-usage/{stationId}/today
**Bearer token required** (ROLE_ADMIN or ROLE_STAFF)

Get station usage for today.

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "stationName": "Station A",
    "address": "123 Main Street",
    "date": "2025-10-15",
    "totalChargingPoints": 10,
    "currentInUsePoints": 4,
    "currentAvailablePoints": 6,
    "currentUsagePercent": 40.0,
    "totalSessionsToday": 25,
    "completedSessionsToday": 20,
    "activeSessionsToday": 4,
    "totalEnergyToday": 345.8,
    "totalRevenueToday": 1250000.0,
    "peakHour": 14,
    "peakUsagePercent": 80.0
  }
}
```

**Error Codes:**
- `4001`: Station not found

---

### GET /api/station-usage/{stationId}?date={date}
**Bearer token required** (ROLE_ADMIN or ROLE_STAFF)

Get station usage for a specific date.

**Query Parameters:**
- `date` (optional): YYYY-MM-DD format (default: today)

**Example:** `GET /api/station-usage/{stationId}?date=2025-10-14`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "stationId": "uuid-...",
    "stationName": "Station A",
    "address": "123 Main Street",
    "date": "2025-10-14",
    "totalChargingPoints": 10,
    "currentInUsePoints": 0,
    "currentAvailablePoints": 10,
    "currentUsagePercent": 0.0,
    "totalSessionsToday": 28,
    "completedSessionsToday": 28,
    "activeSessionsToday": 0,
    "totalEnergyToday": 378.5,
    "totalRevenueToday": 1350000.0,
    "peakHour": 15,
    "peakUsagePercent": 90.0
  }
}
```

---

### GET /api/station-usage/all/today
**Bearer token required** (ROLE_ADMIN)

Get usage for all stations today.

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "stationName": "Station A",
      "address": "123 Main Street",
      "date": "2025-10-15",
      "totalChargingPoints": 10,
      "currentInUsePoints": 4,
      "currentAvailablePoints": 6,
      "currentUsagePercent": 40.0,
      "totalSessionsToday": 25,
      "completedSessionsToday": 20,
      "activeSessionsToday": 4,
      "totalEnergyToday": 345.8,
      "totalRevenueToday": 1250000.0,
      "peakHour": 14,
      "peakUsagePercent": 80.0
    }
  ]
}
```

---

### GET /api/station-usage/all?date={date}
**Bearer token required** (ROLE_ADMIN)

Get usage for all stations on a specific date.

**Query Parameters:**
- `date` (optional): YYYY-MM-DD format (default: today)

**Example:** `GET /api/station-usage/all?date=2025-10-14`

**Response:** Same format as `/all/today` but for the specified date.

---

## 13. Revenue

### GET /api/revenue/weekly?year={year}&month={month}&week={week}
**Bearer token required** (ROLE_ADMIN)

Get weekly revenue by station.

**Query Parameters:**
- `year` (optional): Default current year
- `month` (optional): Default current month
- `week` (optional): Week number (1-5), default current week

**Example:** `GET /api/revenue/weekly?year=2025&month=10&week=2`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "stationName": "Station A",
      "address": "123 Main Street",
      "month": 10,
      "year": 2025,
      "totalRevenue": 850000.0,
      "totalSessions": 45
    }
  ]
}
```

---

### GET /api/revenue/monthly?year={year}&month={month}
**Bearer token required** (ROLE_ADMIN)

Get monthly revenue by station.

**Query Parameters:**
- `year` (optional): Default current year
- `month` (optional): Default current month (1-12)

**Example:** `GET /api/revenue/monthly?year=2025&month=10`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "stationName": "Station A",
      "address": "123 Main Street",
      "month": 10,
      "year": 2025,
      "totalRevenue": 3450000.0,
      "totalSessions": 185
    },
    {
      "stationId": "uuid-...",
      "stationName": "Station B",
      "address": "456 Second Street",
      "month": 10,
      "year": 2025,
      "totalRevenue": 2890000.0,
      "totalSessions": 142
    }
  ]
}
```

---

### GET /api/revenue/yearly?year={year}
**Bearer token required** (ROLE_ADMIN)

Get yearly revenue by station (all months in the year).

**Query Parameters:**
- `year` (optional): Default current year

**Example:** `GET /api/revenue/yearly?year=2025`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "stationId": "uuid-...",
      "stationName": "Station A",
      "address": "123 Main Street",
      "month": null,
      "year": 2025,
      "totalRevenue": 42500000.0,
      "totalSessions": 2245
    }
  ]
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| **1xxx - General** | | |
| 1000 | Success | Request successful |
| 1001 | Uncategorized error | Unknown error occurred |
| 1002 | Invalid key | Invalid request parameter |
| 1003 | User existed | Email already registered |
| 1004 | User not existed | User not found |
| 1005 | User not found | User not found |
| 1006 | Unauthenticated | Not logged in or invalid token |
| 1007 | Unauthorized | No permission to access resource |
| 1008 | Invalid DOB | Date of birth is invalid |
| **2xxx - Vehicles** | | |
| 2001 | License plate existed | License plate already registered |
| 2002 | Vehicle not found | Vehicle not found |
| 2003 | Vehicle not owned | User doesn't own this vehicle |
| **3xxx - Plans** | | |
| 3001 | Plan not found | Plan not found |
| 3002 | Plan name existed | Plan name already exists |
| 3003 | Invalid plan config | Plan configuration violates billing type rules |
| 3004 | Plan in use | Cannot delete plan that is in use |
| **4xxx - Stations** | | |
| 4001 | Station not found | Station not found |
| 4002 | Staff not found | Staff not found |
| 4003 | Staff already assigned | Staff already assigned to a station |
| 4004 | Staff not in station | Staff not assigned to this station |
| **5xxx - Subscriptions** | | |
| 5001 | Subscription not found | Subscription not found |
| 5002 | Payment method required | Payment method required for paid plans |
| 5003 | Active subscription exists | User already has an active subscription |

---

## Authentication Header

Most endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

To obtain a token, use `/api/auth/login` endpoint.

**Token Expiration:** Tokens expire after a certain period. Use `/api/auth/introspect` to check validity.

---

## Data Formats & Standards

### Date & Time
- **DateTime fields:** ISO 8601 format: `2025-10-15T10:30:00`
- **Date fields:** `YYYY-MM-DD` format: `2025-10-15`

### Numbers
- **Monetary values:** Float/Double in VND (Vietnamese Dong)
- **Energy values:** Float/Double in kWh (kilowatt-hours)
- **Power values:** Float in kW (kilowatts)
- **Percentage values:** Float 0.0 - 100.0

### Enum Values

**Billing Types:**
- `PAY_AS_YOU_GO` - Pay per usage (monthlyFee = 0)
- `MONTHLY_SUBSCRIPTION` - Fixed monthly fee (monthlyFee > 0)
- `VIP` - Premium plan with perks (monthlyFee > 0)

**Station Status:**
- `OPERATIONAL` - Hoạt động bình thường
- `MAINTENANCE` - Đang bảo trì
- `OUT_OF_SERVICE` - Ngưng hoạt động
- `CLOSED` - Đã đóng cửa

**Charging Point Status:**
- `AVAILABLE` - Sẵn sàng
- `OCCUPIED` - Đang sử dụng
- `OUT_OF_SERVICE` - Hỏng
- `MAINTENANCE` - Bảo trì

**Charging Session Status:**
- `ACTIVE` - Đang sạc
- `COMPLETED` - Hoàn thành
- `CANCELLED` - Đã hủy
- `FAILED` - Lỗi

**Notification Types:**
- `CHARGING_COMPLETE` - Hoàn thành sạc
- `LOW_BATTERY` - Pin yếu
- `PROMOTIONAL` - Khuyến mãi
- `MAINTENANCE` - Bảo trì trạm

**Notification Channels:**
- `EMAIL` - Gửi qua email
- `SMS` - Gửi qua tin nhắn

**Payment Method Types:**
- `CREDIT_CARD` - Thẻ tín dụng
- `DEBIT_CARD` - Thẻ ghi nợ
- `E_WALLET` - Ví điện tử

**Roles:**
- `ADMIN` - Quản trị viên
- `STAFF` - Nhân viên
- `DRIVER` - Tài xế

---

## Frontend Integration Tips

### Notification Settings UI

Based on the UI requirements, implement notification settings with:

| UI Label (Vietnamese) | Backend Enum | Channels |
|----------------------|-------------|----------|
| Hoàn thành sạc | `CHARGING_COMPLETE` | EMAIL, SMS |
| Pin yếu | `LOW_BATTERY` | EMAIL, SMS |
| Khuyến mãi | `PROMOTIONAL` | EMAIL, SMS |
| Bảo trì trạm | `MAINTENANCE` | EMAIL, SMS |

**Total: 4 types × 2 channels = 8 toggle switches**

**Implementation:**
- **Individual toggle:** Call `PATCH /api/notification/single` when user toggles a switch
- **Save all button:** Call `PUT /api/notification` with all 8 settings when user clicks "Lưu cài đặt"

### Station Management

- Use `/api/stations/overview` for listing (lightweight)
- Use `/api/stations/detail` for management dashboard with full info
- Use `/api/stations` for CRUD operations

### Dashboard Widgets

- **Driver Dashboard:** Use `/api/charging-sessions/my-dashboard` for overview cards
- **Admin Dashboard:** Use `/api/overview` for system-wide statistics
- **Charts:** Use `/api/dashboard/hourly-sessions` and `/api/charging-sessions/my-analytics/monthly`

---

## Base URL Configuration

**Development:** `http://localhost:8080/evchargingstation`  
**Production:** Update according to deployment environment

---

**Documentation Version:** 2.0  
**Last Updated:** October 15, 2025  
**API Version:** 1.0

---

**Note:** All null values in responses indicate no data available. The API does not fabricate data - if a field is null, it means there's genuinely no data for that field.
