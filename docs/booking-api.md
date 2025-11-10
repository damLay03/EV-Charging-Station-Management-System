# Booking API Documentation

## Authentication Required
All booking endpoints require JWT authentication token in the request header.

## 1. Check Availability

**Endpoint:** `GET /api/bookings/availability`

**Description:** Check if a charging point is available for booking at a specific time.

**Query Parameters:**
- `chargingPointId` (String, required): ID of the charging point
- `bookingTime` (LocalDateTime, required): Desired booking time (ISO 8601 format)
- `vehicleId` (String, required): ID of the vehicle

**Request Example:**
```
GET /api/bookings/availability?chargingPointId=0852615d-bc22-11f0-84a4-a2aa7d0d0e9c&bookingTime=2025-11-10T23:00:00&vehicleId=472ffd0d-bc28-11f0-84a4-a2aa7d0d0e9c
Authorization: Bearer <your-jwt-token>
```

**Response Example:**
```json
{
  "available": true,
  "maxChargePercentage": 57.01,
  "message": "You can only charge up to 57.01% (session will end at 2025-11-11T00:00:00 for the next user)."
}
```

## 2. Create Booking

**Endpoint:** `POST /api/bookings`

**Description:** Create a new booking reservation. Requires a deposit of 50,000 VND from the user's wallet.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <your-jwt-token>
```

**Request Body:**
```json
{
  "vehicleId": "472ffd0d-bc28-11f0-84a4-a2aa7d0d0e9c",
  "chargingPointId": "0852615d-bc22-11f0-84a4-a2aa7d0d0e9c",
  "bookingTime": "2025-11-10T23:00:00",
  "desiredPercentage": 40
}
```

**Response Example (Success):**
```json
{
  "id": 1,
  "user": {
    "userId": "...",
    "email": "user@example.com"
  },
  "vehicle": {
    "vehicleId": "472ffd0d-bc28-11f0-84a4-a2aa7d0d0e9c",
    "model": "VF8"
  },
  "chargingPoint": {
    "pointId": "0852615d-bc22-11f0-84a4-a2aa7d0d0e9c",
    "name": "TS1"
  },
  "bookingTime": "2025-11-10T23:00:00",
  "estimatedEndTime": "2025-11-11T01:30:00",
  "desiredPercentage": 40.0,
  "depositAmount": 50000.0,
  "bookingStatus": "CONFIRMED",
  "createdAt": "2025-11-10T22:00:00"
}
```

**Error Responses:**

1. **Unauthorized (401)**
```json
{
  "code": 9002,
  "message": "Unauthorized Access"
}
```
*Cause:* No JWT token provided in Authorization header or token is invalid.

2. **User Not Found (404)**
```json
{
  "code": 1005,
  "message": "User Not Found"
}
```
*Cause:* The email from JWT token doesn't match any user in the database.

3. **Insufficient Funds (400)**
```json
{
  "code": 8001,
  "message": "Insufficient Funds"
}
```
*Cause:* User's wallet balance is less than 50,000 VND.

4. **Validation Failed (400)**
```json
{
  "code": 9001,
  "message": "Validation Failed"
}
```
*Possible causes:*
- Booking time is in the past
- Booking time is more than 24 hours in the future
- Desired percentage exceeds maximum available percentage

5. **Charging Point Not Found (404)**
```json
{
  "code": 4001,
  "message": "Charging Point Not Found"
}
```

6. **Vehicle Not Found (404)**
```json
{
  "code": 5001,
  "message": "Vehicle Not Found"
}
```

## Important Notes

### Authentication
**You MUST include the JWT token in the Authorization header:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwic2NvcGUiOiJEUklWRVIiLCJpYXQiOjE2OTk2MjM2MDB9.xxx
```

### How to get JWT token?
1. Login via `/api/auth/login` endpoint
2. The response will contain an `access_token` field
3. Use this token in all subsequent requests

### Example using JavaScript (Fetch API):
```javascript
const token = localStorage.getItem('access_token'); // Get token from storage

const response = await fetch('http://localhost:8080/api/bookings', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}` // Important!
  },
  body: JSON.stringify({
    vehicleId: "472ffd0d-bc28-11f0-84a4-a2aa7d0d0e9c",
    chargingPointId: "0852615d-bc22-11f0-84a4-a2aa7d0d0e9c",
    bookingTime: "2025-11-10T23:00:00",
    desiredPercentage: 40
  })
});

const data = await response.json();
```

### Example using Axios:
```javascript
import axios from 'axios';

const token = localStorage.getItem('access_token');

const response = await axios.post('http://localhost:8080/api/bookings', 
  {
    vehicleId: "472ffd0d-bc28-11f0-84a4-a2aa7d0d0e9c",
    chargingPointId: "0852615d-bc22-11f0-84a4-a2aa7d0d0e9c",
    bookingTime: "2025-11-10T23:00:00",
    desiredPercentage: 40
  },
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

## Business Rules

### Booking Constraints
1. **Time Window:** Can only book up to 24 hours in advance
2. **Deposit:** 50,000 VND required (deducted from wallet)
3. **Check-in Window:** ±10 minutes from booking time
4. **Max Charge:** Limited by next booking slot

### Booking Status
- `CONFIRMED`: Successfully booked, deposit paid
- `IN_PROGRESS`: User arrived and charging started
- `COMPLETED`: Charging session completed
- `EXPIRED`: Missed check-in window (deposit forfeited)
- `CANCELLED_BY_USER`: User cancelled (future feature)

### Deposit Handling
- **On time arrival:** Deposit applied to charging fee
- **Late/No show:** Deposit forfeited after 10 minutes
- **Charging cost < Deposit:** Difference refunded to wallet

