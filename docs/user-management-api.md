# User Management API Documentation

## Tổng quan

API quản lý người dùng cho phép đăng ký tài khoản mới, xem và cập nhật thông tin cá nhân, cũng như quản lý người dùng (dành cho ADMIN).

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token (trừ endpoint đăng ký)

---

## API Endpoints

### DRIVER APIs

#### 1. Đăng ký tài khoản mới

**Endpoint**: `POST /api/users/register`

**Mô tả**: Tạo tài khoản DRIVER mới trong hệ thống.

**Quyền truy cập**: Public (không cần authentication)

**Request Body**:
```json
{
  "fullName": "Nguyễn Văn A",
  "email": "nguyenvana@example.com",
  "password": "Password123!",
  "phoneNumber": "0901234567",
  "address": "123 Đường ABC, Quận 1, TP.HCM"
}
```

**Request Fields**:
- `fullName` (string, required): Họ và tên đầy đủ
- `email` (string, required): Email (phải unique, format hợp lệ)
- `password` (string, required): Mật khẩu (tối thiểu 8 ký tự)
- `phoneNumber` (string, required): Số điện thoại
- `address` (string, optional): Địa chỉ

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0901234567",
    "role": "DRIVER"
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "code": 1002,
  "message": "Email already exists"
}
```

---

#### 2. Xem thông tin cá nhân (Driver)

**Endpoint**: `GET /api/users/driver/myInfo`

**Mô tả**: Driver xem thông tin chi tiết của chính mình.

**Quyền truy cập**: DRIVER (Bearer token)

**Request**: Không cần body, userId lấy từ JWT token

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0901234567",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

#### 3. Cập nhật thông tin cá nhân (Driver)

**Endpoint**: `PATCH /api/users/driver/myInfo`

**Mô tả**: Driver tự cập nhật thông tin của mình (không thể sửa email).

**Quyền truy cập**: DRIVER (Bearer token)

**Request Body** (tất cả các field đều optional):
```json
{
  "fullName": "Nguyễn Văn A Updated",
  "phoneNumber": "0912345678",
  "address": "456 Đường XYZ, Quận 2, TP.HCM"
}
```

**Request Fields**:
- `fullName` (string, optional): Họ tên mới
- `phoneNumber` (string, optional): Số điện thoại mới
- `address` (string, optional): Địa chỉ mới
- `password` (string, optional): Mật khẩu mới (nếu muốn đổi)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A Updated",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0912345678",
    "address": "456 Đường XYZ, Quận 2, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

### ADMIN APIs

#### 4. Lấy danh sách tất cả người dùng

**Endpoint**: `GET /api/users`

**Mô tả**: ADMIN xem danh sách tất cả driver trong hệ thống.

**Quyền truy cập**: ADMIN (Bearer token)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "user-uuid-123",
      "fullName": "Nguyễn Văn A",
      "email": "nguyenvana@example.com",
      "phoneNumber": "0901234567",
      "role": "DRIVER",
      "joinDate": "2024-01-15T10:30:00",
      "totalSessions": 25,
      "totalSpent": 1250000.0
    },
    {
      "id": "user-uuid-456",
      "fullName": "Trần Thị B",
      "email": "tranthib@example.com",
      "phoneNumber": "0987654321",
      "role": "DRIVER",
      "joinDate": "2024-02-20T14:15:00",
      "totalSessions": 10,
      "totalSpent": 500000.0
    }
  ]
}
```

---

#### 5. Xem chi tiết một driver

**Endpoint**: `GET /api/users/driver/{driverId}/info`

**Mô tả**: ADMIN xem thông tin đầy đủ của một driver cụ thể.

**Quyền truy cập**: ADMIN (Bearer token)

**Path Parameters**:
- `driverId` (string, required): ID của driver cần xem

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0901234567",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

#### 6. Cập nhật thông tin driver (Admin)

**Endpoint**: `PUT /api/users/driver/{driverId}`

**Mô tả**: ADMIN cập nhật thông tin của driver (không thể sửa email, password, joinDate).

**Quyền truy cập**: ADMIN (Bearer token)

**Path Parameters**:
- `driverId` (string, required): ID của driver cần cập nhật

**Request Body**:
```json
{
  "fullName": "Nguyễn Văn A Modified",
  "phoneNumber": "0909090909",
  "address": "789 Đường MNO, Quận 3, TP.HCM"
}
```

**Request Fields**:
- `fullName` (string, optional): Họ tên mới
- `phoneNumber` (string, optional): Số điện thoại mới
- `address` (string, optional): Địa chỉ mới

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A Modified",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0909090909",
    "address": "789 Đường MNO, Quận 3, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

#### 7. Xóa người dùng

**Endpoint**: `DELETE /api/users/{userId}`

**Mô tả**: ADMIN xóa người dùng khỏi hệ thống (hard delete).

**Quyền truy cập**: ADMIN (Bearer token)

**Path Parameters**:
- `userId` (string, required): ID của user cần xóa

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Deleted"
}
```

**Lưu ý**: Việc xóa user sẽ ảnh hưởng đến dữ liệu liên quan (vehicles, charging sessions, etc.). Cần cân nhắc kỹ trước khi xóa.

---

#### 8. Gán role cho người dùng

**Endpoint**: `PUT /api/users/{userId}/role`

**Mô tả**: ADMIN gán role (DRIVER, STAFF, ADMIN) cho người dùng.

**Quyền truy cập**: ADMIN (Bearer token)

**Path Parameters**:
- `userId` (string, required): ID của user cần gán role

**Request Body**:
```json
{
  "role": "STAFF"
}
```

**Request Fields**:
- `role` (string, required): Role mới (DRIVER | STAFF | ADMIN)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0901234567",
    "role": "STAFF"
  }
}
```

---

#### 9. Lấy thông tin user theo ID

**Endpoint**: `GET /api/users/{userId}`

**Mô tả**: Lấy thông tin cơ bản của một user theo ID.

**Quyền truy cập**: Authenticated (Bearer token)

**Path Parameters**:
- `userId` (string, required): ID của user

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid-123",
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phoneNumber": "0901234567",
    "role": "DRIVER"
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ (email đã tồn tại, format sai, v.v.) |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | User not found | Không tìm thấy người dùng |

---

## Lưu ý

1. **Email unique**: Email phải là duy nhất trong hệ thống, không được trùng lặp.

2. **Password security**: 
   - Mật khẩu được mã hóa (hash) trước khi lưu vào database
   - Không thể xem mật khẩu gốc của người dùng
   - Driver chỉ có thể đổi mật khẩu của chính mình

3. **Role hierarchy**:
   - ADMIN: Có quyền cao nhất, quản lý toàn bộ hệ thống
   - STAFF: Quản lý trạm sạc được gán
   - DRIVER: Người dùng thông thường, sử dụng dịch vụ sạc xe

4. **Self-update vs Admin-update**:
   - Driver tự update: dùng `PATCH /api/users/driver/myInfo`
   - Admin update driver: dùng `PUT /api/users/driver/{driverId}`
   - Không cho phép driver tự thay đổi role hoặc email

