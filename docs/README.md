# EV Charging Station Management System - API Documentation

## Tổng quan

Đây là tài liệu API đầy đủ cho hệ thống quản lý trạm sạc xe điện. Tài liệu được tổ chức theo từng module chức năng để dễ dàng tra cứu và sử dụng.

**Base URL**: `http://localhost:8080`

**Environment**: Development

---

## Danh sách tài liệu API

### 1. Authentication & Authorization
- **[Authentication API](./authentication-api.md)** - Đăng nhập, xác thực JWT token
- **[User Management API](./user-management-api.md)** - Đăng ký, quản lý thông tin người dùng, phân quyền

### 2. Vehicle Management
- **[Vehicle API](./vehicles-api.md)** - Quản lý xe điện của driver (CRUD, danh sách hãng xe, model)

### 3. Station & Charging Point Management
- **[Station Management API](./station-management-api.md)** - Quản lý trạm sạc và trụ sạc (CRUD, trạng thái, staff assignment)

### 4. Charging Session
- **[Charging Simulation API](./charging-simulation-api.md)** - Start/Stop phiên sạc, mô phỏng sạc, lịch sử

### 5. Plan Management
- **[Plan API](./plan-api.md)** - Quản lý gói dịch vụ (CRUD plans)

### 6. Payment
- **[Payment Method API](./payment-method-api.md)** - Quản lý phương thức thanh toán của driver

### 7. Dashboard & Analytics
- **[Driver Dashboard API](./dashboard-driver-api.md)** - Thống kê và phân tích cho driver
- **[Staff Dashboard API](./staff-dashboard-api.md)** - Dashboard cho staff quản lý trạm
- **[Admin Dashboard API](./admin-dashboard-api.md)** - Tổng quan hệ thống, doanh thu, sử dụng trạm

### 8. Notification
- **[Notification Settings API](./notification-settings-api.md)** - Cài đặt thông báo

---

## Cấu trúc Response chung

Tất cả API response đều tuân theo cấu trúc:

### Success Response
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    // Data object hoặc array
  }
}
```

### Error Response
```json
{
  "code": 1002,
  "message": "Error description here"
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1001 | Uncategorized error | Lỗi chưa được phân loại |
| 1002 | Invalid data | Dữ liệu không hợp lệ |
| 1003 | User not existed | Người dùng không tồn tại |
| 1004 | Unauthenticated | Chưa đăng nhập hoặc token không hợp lệ |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Resource not found | Không tìm thấy tài nguyên |
| 1007 | Invalid request | Request không hợp lệ |
| 1008 | Payment method required | Thiếu phương thức thanh toán |
| 1009 | Already exists | Tài nguyên đã tồn tại |
| 1010 | Staff not assigned | Staff chưa được gán trạm |

---

## Authentication

Hầu hết các API yêu cầu authentication bằng JWT token (trừ các public API).

### Cách sử dụng

1. Đăng nhập qua `/api/auth/login` để lấy token
2. Thêm token vào header của mọi request tiếp theo:

```
Authorization: Bearer {your_jwt_token}
```

### Token Claims

JWT token chứa các thông tin:
- `userId`: ID của người dùng
- `email`: Email
- `scope`: Roles (ADMIN, STAFF, DRIVER)
- `iat`: Issued at (thời gian phát hành)
- `exp`: Expiration (thời gian hết hạn)

---

## Roles & Permissions

Hệ thống có 3 roles chính:

### ADMIN
- Quản lý toàn bộ hệ thống
- CRUD users, stations, charging points, plans
- Xem báo cáo và analytics
- Gán staff cho trạm

### STAFF
- Quản lý trạm được gán
- Xử lý thanh toán tại trạm
- Báo cáo và giải quyết sự cố
- Xem thống kê trạm

### DRIVER
- Quản lý xe và thông tin cá nhân
- Bắt đầu/kết thúc phiên sạc
- Xem dashboard và lịch sử
- Quản lý phương thức thanh toán

---

## Request & Response Format

### Content-Type
Tất cả request và response đều sử dụng `application/json`

### Date Format
- Datetime: ISO 8601 format (`yyyy-MM-dd'T'HH:mm:ss`)
- Date only: `yyyy-MM-dd`

### Timezone
- Tất cả datetime theo UTC
- Frontend cần convert sang local timezone

### Pagination
Một số API hỗ trợ pagination (sẽ được mở rộng):
```
GET /api/resource?page=0&size=20&sort=createdAt,desc
```

---

## Testing với Swagger UI

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Cách test API với authentication:

1. Mở Swagger UI
2. Đăng nhập qua `/api/auth/login` để lấy token
3. Click nút **Authorize** ở góc trên bên phải
4. Nhập: `Bearer {token}`
5. Click Authorize
6. Giờ có thể test các protected endpoints

---

## Versioning

Hiện tại: **v1.0**

API versioning sẽ được thêm vào URL khi có breaking changes:
- Current: `/api/users`
- Future: `/api/v2/users`

---

## Rate Limiting

Chưa implement rate limiting trong version hiện tại.

Dự kiến:
- Public endpoints: 100 requests/minute
- Authenticated endpoints: 1000 requests/minute

---

## CORS

CORS được cấu hình cho phép:
- All origins trong development
- Specific origins trong production

---

## Support & Contact

- **Backend Team**: backend@evcharging.com
- **API Issues**: Tạo issue trên Git repository
- **Documentation Updates**: Liên hệ team lead

---

## Changelog

### Version 1.0 (2025-10-25)
- Initial API documentation
- 10+ API modules
- Authentication với JWT
- Role-based access control
- Charging simulation
- Dashboard và analytics

---

## Notes cho Frontend Team

1. **Error Handling**: 
   - Luôn check `code` trong response
   - Hiển thị `message` cho user khi có lỗi
   - Handle 401 (redirect to login) và 403 (show permission error)

2. **Loading States**:
   - Show loading spinner khi call API
   - Disable buttons để tránh double-submit

3. **Validation**:
   - Validate ở frontend trước khi gửi request
   - Backend vẫn validate lại để đảm bảo data integrity

4. **Caching**:
   - Cache static data (brands, models, plans)
   - Refresh cache khi cần thiết

5. **Real-time Updates**:
   - Dashboard data: refresh mỗi 30-60 giây
   - Charging session: polling hoặc WebSocket (tùy implement)

6. **Datetime Handling**:
   - Luôn convert UTC sang local timezone khi hiển thị
   - Gửi lên server theo UTC

7. **File Upload**:
   - Chưa có API upload file trong version hiện tại
   - Sẽ được thêm cho avatar, station images

---

## Quick Start Guide

### 1. Đăng ký tài khoản
```bash
POST /api/users/register
{
  "fullName": "Test User",
  "email": "test@example.com",
  "password": "Test123!",
  "phoneNumber": "0901234567"
}
```

### 2. Đăng nhập
```bash
POST /api/auth/login
{
  "email": "test@example.com",
  "password": "Test123!"
}
```

### 3. Thêm xe
```bash
POST /api/vehicles
Headers: Authorization: Bearer {token}
{
  "model": "VINFAST_VF8",
  "licensePlate": "51A-12345",
  "currentSocPercent": 50
}
```

### 4. Xem danh sách trạm
```bash
GET /api/stations?status=OPERATIONAL
```

### 5. Bắt đầu sạc
```bash
POST /api/charging-sessions/start
Headers: Authorization: Bearer {token}
{
  "chargingPointId": "cp-uuid-1",
  "vehicleId": "vehicle-uuid-1",
  "targetSocPercent": 80
}
```

---
