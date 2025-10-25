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
  "email": "nguyenvana@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!"
}
```

**Request Fields**:
- `email` (string, required): Email (phải unique, format hợp lệ)
- `password` (string, required): Mật khẩu (tối thiểu 6 ký tự)
- `confirmPassword` (string, required): Xác nhận mật khẩu (phải khớp với password)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": null,
    "dateOfBirth": null,
    "gender": null,
    "firstName": null,
    "lastName": null,
    "fullName": null,
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
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": "0901234567",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "firstName": "Văn",
    "lastName": "Nguyễn",
    "fullName": "Nguyễn Văn A",
    "role": "DRIVER",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

**Response Fields**:
- `userId` (string): ID của user
- `email` (string): Email
- `phone` (string, nullable): Số điện thoại
- `dateOfBirth` (string, nullable): Ngày sinh (yyyy-MM-dd)
- `gender` (string, nullable): Giới tính (MALE | FEMALE | OTHER)
- `firstName` (string, nullable): Tên
- `lastName` (string, nullable): Họ
- `fullName` (string, nullable): Họ và tên đầy đủ
- `role` (string): Vai trò
- `address` (string, nullable): Địa chỉ (chỉ có trong DriverResponse)
- `joinDate` (string, nullable): Ngày tham gia (chỉ có trong DriverResponse)

---

#### 3. Cập nhật thông tin cá nhân (Driver)

**Endpoint**: `PATCH /api/users/driver/myInfo`

**Mô tả**: Driver tự cập nhật thông tin của mình (không thể sửa email).

**Quyền truy cập**: DRIVER (Bearer token)

**Request Body** (tất cả các field đều optional):
```json
{
  "phone": "0912345678",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "firstName": "Văn",
  "lastName": "Nguyễn",
  "address": "456 Đường XYZ, Quận 2, TP.HCM"
}
```

**Request Fields**:
- `phone` (string, optional): Số điện thoại mới
- `dateOfBirth` (string, optional): Ngày sinh (yyyy-MM-dd)
- `gender` (string, optional): Giới tính (MALE | FEMALE | OTHER)
- `firstName` (string, optional): Tên
- `lastName` (string, optional): Họ
- `address` (string, optional): Địa chỉ mới

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": "0912345678",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "firstName": "Văn",
    "lastName": "Nguyễn",
    "fullName": "Nguyễn Văn",
    "role": "DRIVER",
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
      "fullName": "Nguyễn Văn A",
      "email": "nguyenvana@example.com",
      "phone": "0901234567",
      "joinDate": "2024-01-15",
      "planName": "Gói Tiêu chuẩn",
      "sessionCount": 25,
      "totalSpent": 1250000.0,
      "status": "Hoạt động",
      "isActive": true
    },
    {
      "fullName": "Trần Thị B",
      "email": "tranthib@example.com",
      "phone": "0987654321",
      "joinDate": "2024-02-20",
      "planName": "Gói Premium",
      "sessionCount": 10,
      "totalSpent": 500000.0,
      "status": "Hoạt động",
      "isActive": true
    }
  ]
}
```

**Response Fields** (mỗi user):
- `fullName` (string, nullable): Họ và tên đầy đủ
- `email` (string): Email
- `phone` (string, nullable): Số điện thoại
- `joinDate` (string, nullable): Ngày tham gia (yyyy-MM-dd)
- `planName` (string, nullable): Tên gói dịch vụ đang sử dụng
- `sessionCount` (integer, nullable): Số phiên sạc đã thực hiện
- `totalSpent` (number, nullable): Tổng chi tiêu
- `status` (string, nullable): Trạng thái text
- `isActive` (boolean, nullable): Trạng thái hoạt động

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
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": "0901234567",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "firstName": "Văn",
    "lastName": "Nguyễn",
    "fullName": "Nguyễn Văn A",
    "role": "DRIVER",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

#### 6. Cập nhật thông tin driver (Admin)

**Endpoint**: `PUT /api/users/driver/{driverId}`

**Mô tả**: ADMIN cập nhật thông tin driver (không thể sửa email, password, joinDate).

**Quyền truy cập**: ADMIN (Bearer token)

**Path Parameters**:
- `driverId` (string, required): ID của driver

**Request Body** (tất cả fields đều optional):
```json
{
  "phone": "0912345678",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "firstName": "Văn",
  "lastName": "Nguyễn",
  "address": "456 Đường XYZ, Quận 2, TP.HCM"
}
```

**Request Fields**:
- `phone` (string, optional): Số điện thoại
- `dateOfBirth` (string, optional): Ngày sinh (yyyy-MM-dd)
- `gender` (string, optional): Giới tính (MALE | FEMALE | OTHER)
- `firstName` (string, optional): Tên
- `lastName` (string, optional): Họ
- `address` (string, optional): Địa chỉ

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": "0912345678",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "firstName": "Văn",
    "lastName": "Nguyễn",
    "fullName": "Nguyễn Văn",
    "role": "DRIVER",
    "address": "456 Đường XYZ, Quận 2, TP.HCM",
    "joinDate": "2024-01-15T10:30:00"
  }
}
```

---

#### 7. Xem thông tin một user bất kỳ

**Endpoint**: `GET /api/users/{userId}`

**Mô tả**: Lấy thông tin cơ bản của một user.

**Quyền truy cập**: Authenticated

**Path Parameters**:
- `userId` (string, required): ID của user

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "userId": "user-uuid-123",
    "email": "nguyenvana@example.com",
    "phone": "0901234567",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "firstName": "Văn",
    "lastName": "Nguyễn",
    "fullName": "Nguyễn Văn A",
    "role": "DRIVER"
  }
}
```

---

#### 8. Xóa user

**Endpoint**: `DELETE /api/users/{userId}`

**Mô tả**: ADMIN xóa user khỏi hệ thống (hard delete).

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

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Not found | Không tìm thấy user |

---

## Lưu ý khi sử dụng

1. **Đăng ký**: Chỉ cần email và password, các thông tin khác có thể cập nhật sau
2. **fullName**: Được tự động tạo từ firstName + lastName
3. **Gender**: Phải là một trong các giá trị: MALE, FEMALE, OTHER
4. **Partial Update**: PATCH endpoint chỉ cập nhật các field được gửi lên, field null sẽ không thay đổi
5. **Admin Privileges**: Admin có thể xem và sửa thông tin tất cả driver nhưng không thể thay đổi email và password
