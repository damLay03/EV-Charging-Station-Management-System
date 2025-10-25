# Authentication API Documentation

## Tổng quan

API xác thực sử dụng JWT (JSON Web Token) để bảo mật. Sau khi đăng nhập thành công, client sẽ nhận được token và sử dụng token này cho các request tiếp theo.

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`

---

## API Endpoints

### 1. Đăng nhập (Login)

**Endpoint**: `POST /api/auth/login`

**Mô tả**: Xác thực người dùng và trả về JWT token.

**Quyền truy cập**: Public (không cần authentication)

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "yourPassword123"
}
```

**Request Fields**:
- `email` (string, required): Email đăng ký của người dùng
- `password` (string, required): Mật khẩu

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjk...",
    "authenticated": true,
    "userInfo": {
      "userId": "user-uuid-123",
      "email": "user@example.com",
      "phone": "0901234567",
      "dateOfBirth": "1990-01-15",
      "gender": "MALE",
      "firstName": "Văn",
      "lastName": "Nguyễn",
      "fullName": "Nguyễn Văn A",
      "role": "DRIVER"
    }
  }
}
```

**Response Fields**:
- `token` (string): JWT token để sử dụng cho các request tiếp theo
- `authenticated` (boolean): Trạng thái xác thực
- `userInfo` (object): Thông tin chi tiết của user
  - `userId` (string): ID của user
  - `email` (string): Email
  - `phone` (string, nullable): Số điện thoại
  - `dateOfBirth` (string, nullable): Ngày sinh (yyyy-MM-dd)
  - `gender` (string, nullable): Giới tính (MALE | FEMALE | OTHER)
  - `firstName` (string, nullable): Tên
  - `lastName` (string, nullable): Họ
  - `fullName` (string, nullable): Họ và tên đầy đủ
  - `role` (string): Vai trò (ADMIN | STAFF | DRIVER)

**Error Response** (401 Unauthorized):
```json
{
  "code": 1004,
  "message": "Invalid email or password"
}
```

**Cách sử dụng token**:
Sau khi nhận được token, thêm vào header của các request tiếp theo:
```
Authorization: Bearer {token}
```

---

### 2. Kiểm tra Token (Introspect)

**Endpoint**: `POST /api/auth/introspect`

**Mô tả**: Kiểm tra tính hợp lệ của JWT token.

**Quyền truy cập**: Public

**Request Body**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjk..."
}
```

**Request Fields**:
- `token` (string, required): JWT token cần kiểm tra

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

**Response Fields**:
- `valid` (boolean): `true` nếu token hợp lệ, `false` nếu token hết hạn hoặc không hợp lệ

**Response khi token không hợp lệ**:
```json
{
  "code": 1000,
  "result": {
    "valid": false
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1004 | Unauthenticated | Email hoặc password không đúng |
| 1005 | Unauthorized | Token không hợp lệ hoặc hết hạn |

---

## Lưu ý khi sử dụng

1. **Token expiration**: JWT token có thời hạn sử dụng. Khi token hết hạn, client cần đăng nhập lại để lấy token mới.

2. **Bảo mật token**: 
   - Không lưu token trong localStorage nếu có thể, ưu tiên sử dụng httpOnly cookie
   - Không chia sẻ token với bên thứ ba
   - Xóa token khi người dùng đăng xuất

3. **Header format**: 
   - Luôn sử dụng prefix "Bearer " trước token
   - Ví dụ: `Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...`

4. **Token payload**: Token chứa các claim như:
   - `userId`: ID của người dùng
   - `email`: Email của người dùng
   - `scope`: Các quyền (roles) của người dùng (ADMIN, STAFF, DRIVER)
   - `iat`: Thời gian phát hành
   - `exp`: Thời gian hết hạn
