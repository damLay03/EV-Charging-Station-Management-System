# Incident Management API Documentation

## 📋 Implementation Summary

### ✅ Completed Features (100%)

#### 1. New Enum: IncidentStatus
```java
public enum IncidentStatus {
    WAITING,   // Báo cáo đang chờ được admin duyệt
    WORKING,   // Báo cáo đã được admin duyệt và đang được giải quyết
    DONE       // Báo cáo đã giải quyết xong
}
```
**File:** `src/main/java/com/swp/evchargingstation/enums/IncidentStatus.java`

#### 2. Updated Entity
- **Incident.java**: Changed `status` field from `String` to `IncidentStatus` enum
- Added `@Enumerated(EnumType.STRING)` annotation

#### 3. New Controller & Service
- **IncidentController.java**: Dedicated controller at `/api/incidents`
- **IncidentService.java**: Service layer with role-based methods
- Separated from StaffDashboardController for better organization

#### 4. Files Modified
**Created (3 files):**
- `enums/IncidentStatus.java`
- `service/IncidentService.java`
- `controller/IncidentController.java`

**Modified (5 files):**
- `entity/Incident.java` - Status enum
- `dto/request/IncidentUpdateRequest.java` - Status enum, description field
- `dto/response/IncidentResponse.java` - Status enum
- `controller/StaffDashboardController.java` - Removed incident endpoints
- `service/StaffDashboardService.java` - Removed incident methods

---

## Overview
API để quản lý báo cáo sự cố (incidents) tại các trạm sạc. STAFF có thể tạo và xem báo cáo của station mình, ADMIN có thể quản lý tất cả báo cáo và cập nhật trạng thái.

## Base URL
```
/api/incidents
```

## Incident Status Flow
```
WAITING (Chờ admin duyệt) 
   ↓
WORKING (Admin đã duyệt, đang xử lý) 
   ↓
DONE (Đã giải quyết xong, auto set resolvedAt)
```

### Role-Based Permissions

#### STAFF có thể:
- ✅ Tạo incident report (auto set WAITING)
- ✅ Xem tất cả incidents của station mình
- ✅ Cập nhật mô tả incident
- ❌ KHÔNG thể thay đổi status

#### ADMIN có thể:
- ✅ Xem tất cả incidents của tất cả stations
- ✅ Xem chi tiết bất kỳ incident nào
- ✅ Cập nhật status (WAITING → WORKING → DONE)
- ✅ Cập nhật mô tả
- ✅ Xóa incident

---

## STAFF Endpoints

### 1. Create Incident Report
**POST** `/api/incidents`

Tạo báo cáo sự cố mới tại station của staff. Trạng thái mặc định: **WAITING**.

**Authorization:** STAFF only

**Request Body:**
```json
{
  "stationId": "string",
  "chargingPointId": "string (optional)",
  "description": "string",
  "severity": "LOW | MEDIUM | HIGH | CRITICAL"
}
```

**Response (200 OK):**
```json
{
  "code": 200,
  "message": "Báo cáo sự cố thành công, đang chờ admin duyệt",
  "result": {
    "incidentId": "uuid",
    "reporterName": "string",
    "stationName": "string",
    "chargingPointName": "string",
    "reportedAt": "2025-01-15T10:30:00",
    "description": "string",
    "severity": "HIGH",
    "status": "WAITING",
    "assignedStaffName": "string",
    "resolvedAt": null
  }
}
```

---

### 2. Get My Station Incidents
**GET** `/api/incidents/my-station`

Xem tất cả incidents của station mình quản lý.

**Authorization:** STAFF only

**Response (200 OK):**
```json
{
  "code": 1000,
  "result": [
    {
      "incidentId": "uuid",
      "reporterName": "string",
      "stationName": "string",
      "chargingPointName": "string",
      "reportedAt": "2025-01-15T10:30:00",
      "description": "string",
      "severity": "HIGH",
      "status": "WORKING",
      "assignedStaffName": "string",
      "resolvedAt": null
    }
  ]
}
```

---

### 3. Update Incident Description
**PUT** `/api/incidents/{incidentId}/description`

Cập nhật mô tả của incident (STAFF không thể thay đổi status).

**Authorization:** STAFF only

**Path Parameters:**
- `incidentId`: UUID của incident

**Request Body:**
```json
{
  "description": "Cập nhật: đã kiểm tra, phát hiện vấn đề ở dây sạc"
}
```

**Response (200 OK):**
```json
{
  "code": 200,
  "message": "Cập nhật mô tả sự cố thành công",
  "result": {
    "incidentId": "uuid",
    "reporterName": "string",
    "stationName": "string",
    "chargingPointName": "string",
    "reportedAt": "2025-01-15T10:30:00",
    "description": "Cập nhật: đã kiểm tra, phát hiện vấn đề ở dây sạc",
    "severity": "HIGH",
    "status": "WAITING",
    "assignedStaffName": "string",
    "resolvedAt": null
  }
}
```

---

## ADMIN Endpoints

### 4. Get All Incidents
**GET** `/api/incidents`

Xem tất cả incidents từ tất cả stations.

**Authorization:** ADMIN only

**Response (200 OK):**
```json
{
  "code": 1000,
  "result": [
    {
      "incidentId": "uuid",
      "reporterName": "Staff Name",
      "stationName": "Station A",
      "chargingPointName": "Point 1",
      "reportedAt": "2025-01-15T10:30:00",
      "description": "string",
      "severity": "HIGH",
      "status": "WAITING",
      "assignedStaffName": "string",
      "resolvedAt": null
    },
    {
      "incidentId": "uuid2",
      "reporterName": "Staff Name 2",
      "stationName": "Station B",
      "chargingPointName": "Point 2",
      "reportedAt": "2025-01-14T15:20:00",
      "description": "string",
      "severity": "CRITICAL",
      "status": "WORKING",
      "assignedStaffName": "string",
      "resolvedAt": null
    }
  ]
}
```

---

### 5. Get Incident By ID
**GET** `/api/incidents/{incidentId}`

Xem chi tiết một incident cụ thể.

**Authorization:** ADMIN only

**Path Parameters:**
- `incidentId`: UUID của incident

**Response (200 OK):**
```json
{
  "code": 1000,
  "result": {
    "incidentId": "uuid",
    "reporterName": "string",
    "stationName": "string",
    "chargingPointName": "string",
    "reportedAt": "2025-01-15T10:30:00",
    "description": "string",
    "severity": "HIGH",
    "status": "WORKING",
    "assignedStaffName": "string",
    "resolvedAt": null
  }
}
```

---

### 6. Update Incident Status
**PUT** `/api/incidents/{incidentId}/status`

Cập nhật trạng thái incident. Admin có thể chuyển: WAITING → WORKING → DONE.

**Authorization:** ADMIN only

**Path Parameters:**
- `incidentId`: UUID của incident

**Request Body:**
```json
{
  "status": "WORKING | DONE",
  "description": "Ghi chú thêm về việc xử lý (optional)"
}
```

**Response (200 OK):**
```json
{
  "code": 200,
  "message": "Cập nhật trạng thái sự cố thành công",
  "result": {
    "incidentId": "uuid",
    "reporterName": "string",
    "stationName": "string",
    "chargingPointName": "string",
    "reportedAt": "2025-01-15T10:30:00",
    "description": "string",
    "severity": "HIGH",
    "status": "DONE",
    "assignedStaffName": "string",
    "resolvedAt": "2025-01-16T09:45:00"
  }
}
```

**Note:** Khi status chuyển sang `DONE`, field `resolvedAt` sẽ tự động được set.

---

### 7. Delete Incident
**DELETE** `/api/incidents/{incidentId}`

Xóa một incident report.

**Authorization:** ADMIN only

**Path Parameters:**
- `incidentId`: UUID của incident

**Response (200 OK):**
```json
{
  "code": 1000,
  "message": "Xóa báo cáo sự cố thành công"
}
```

---

## Error Codes

| Code  | Message                  | Description                           |
|-------|--------------------------|---------------------------------------|
| 13001 | Incident Not Found       | Không tìm thấy incident với ID này   |
| 2001  | Station Not Found        | Không tìm thấy station                |
| 2002  | Staff Not Found          | Không tìm thấy staff                  |
| 6002  | Charging Point Not Found | Không tìm thấy charging point         |
| 9998  | Unauthenticated          | Chưa đăng nhập                        |
| 9999  | Unauthorized             | Không có quyền truy cập               |

---

## Status Descriptions

### IncidentStatus Enum
- **WAITING**: Báo cáo đang chờ admin duyệt
- **WORKING**: Báo cáo đã được admin duyệt và đang được giải quyết
- **DONE**: Báo cáo đã được giải quyết xong

### IncidentSeverity Enum
- **LOW**: Mức độ thấp
- **MEDIUM**: Mức độ trung bình
- **HIGH**: Mức độ cao
- **CRITICAL**: Mức độ nghiêm trọng

---

## Migration Notes

### ⚠️ Breaking Changes from Old API

#### API Endpoints Changed
- ❌ **OLD**: `/api/staff/incidents` (đã xóa)
- ✅ **NEW**: `/api/incidents` (controller mới, dedicated)

#### Status Field Type Changed
- ❌ **OLD**: `String` type with values: `"REPORTED"`, `"IN_PROGRESS"`, `"RESOLVED"`, `"CLOSED"`
- ✅ **NEW**: `IncidentStatus` enum with values: `WAITING`, `WORKING`, `DONE`

#### Permission Model Changed
**Old Model:**
- Staff có thể update bất kỳ field nào của incident tại station mình

**New Model:**
- **STAFF**: Chỉ có thể update `description`, KHÔNG thể thay đổi `status`
- **ADMIN**: Có thể update cả `status` và `description`, có thể xóa incident

#### Request/Response Changes
**IncidentUpdateRequest - OLD:**
```json
{
  "status": "RESOLVED",
  "resolution": "Đã sửa xong"
}
```

**IncidentUpdateRequest - NEW:**
```json
{
  "status": "DONE",
  "description": "Đã sửa xong"
}
```

---

### 🔧 Database Migration Required

⚠️ **QUAN TRỌNG**: Nếu database có dữ liệu cũ, BẮT BUỘC phải chạy migration SQL sau:

```sql
-- Migrate old status values to new enum values
UPDATE incidents 
SET status = CASE 
    WHEN status = 'REPORTED' THEN 'WAITING'
    WHEN status = 'IN_PROGRESS' THEN 'WORKING'
    WHEN status IN ('RESOLVED', 'CLOSED') THEN 'DONE'
    ELSE 'WAITING'
END
WHERE status IN ('REPORTED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
```

**Mapping:**
- `REPORTED` → `WAITING`
- `IN_PROGRESS` → `WORKING`
- `RESOLVED` / `CLOSED` → `DONE`

---

## 🔥 Build & Deployment Status

### Compilation Status
✅ **Maven Build**: SUCCESS  
✅ **No Compilation Errors**  
⚠️ **Warnings**: Only IDE "never used" warnings (normal before server runs)

### Files Summary

**Created (3 new files):**
1. `src/main/java/com/swp/evchargingstation/enums/IncidentStatus.java`
2. `src/main/java/com/swp/evchargingstation/service/IncidentService.java`
3. `src/main/java/com/swp/evchargingstation/controller/IncidentController.java`

**Modified (5 files):**
1. `src/main/java/com/swp/evchargingstation/entity/Incident.java`
2. `src/main/java/com/swp/evchargingstation/dto/request/IncidentUpdateRequest.java`
3. `src/main/java/com/swp/evchargingstation/dto/response/IncidentResponse.java`
4. `src/main/java/com/swp/evchargingstation/controller/StaffDashboardController.java`
5. `src/main/java/com/swp/evchargingstation/service/StaffDashboardService.java`

**Removed from StaffDashboardController:**
- `GET /api/staff/incidents` endpoint
- `POST /api/staff/incidents` endpoint
- `PUT /api/staff/incidents/{id}` endpoint

---

## 🚀 Quick Start Guide

### 1. Start Server
```bash
mvn spring-boot:run
```

### 2. Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 3. Test Endpoints
Look for **"Incident Management"** tag in Swagger UI

### 4. Example Usage

#### STAFF - Create Incident
```bash
POST /api/incidents
Authorization: Bearer {staff_token}

{
  "stationId": "station-uuid",
  "chargingPointId": "point-uuid",
  "description": "Charging point không hoạt động",
  "severity": "HIGH"
}
```

#### ADMIN - Update Status
```bash
PUT /api/incidents/{incidentId}/status
Authorization: Bearer {admin_token}

{
  "status": "WORKING",
  "description": "Đang kiểm tra và sửa chữa"
}
```

---

## 📊 Implementation Checklist

| Task | Status | Details |
|------|--------|---------|
| ✅ Create IncidentStatus enum | Done | 3 states: WAITING, WORKING, DONE |
| ✅ Update Incident entity | Done | Changed status to enum type |
| ✅ Update DTOs | Done | IncidentUpdateRequest, IncidentResponse |
| ✅ Create IncidentService | Done | 7 methods with role-based logic |
| ✅ Create IncidentController | Done | 7 endpoints (3 STAFF + 4 ADMIN) |
| ✅ Remove old code | Done | Cleaned up StaffDashboard files |
| ✅ Documentation | Done | This file |
| ✅ Build verification | Done | Maven compile successful |

**Progress: 8/8 tasks completed (100%)** 🎉

