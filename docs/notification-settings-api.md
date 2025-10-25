# Notification Settings API Documentation

## Tổng quan

API quản lý cài đặt thông báo cho phép người dùng tùy chỉnh các loại thông báo họ muốn nhận.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: Authenticated users (DRIVER, STAFF, ADMIN)

---

## Enums

### NotificationType
- `CHARGING_STARTED`: Thông báo khi bắt đầu sạc
- `CHARGING_COMPLETED`: Thông báo khi hoàn thành sạc
- `CHARGING_STOPPED`: Thông báo khi dừng sạc
- `LOW_BATTERY`: Cảnh báo pin yếu
- `PAYMENT_SUCCESS`: Thanh toán thành công
- `PAYMENT_FAILED`: Thanh toán thất bại
- `SUBSCRIPTION_EXPIRING`: Gói sắp hết hạn
- `SUBSCRIPTION_RENEWED`: Gói đã gia hạn
- `STATION_MAINTENANCE`: Trạm bảo trì
- `PROMOTION`: Khuyến mãi

### NotificationChannel
- `EMAIL`: Gửi qua email
- `SMS`: Gửi qua SMS
- `PUSH`: Thông báo push trên app
- `IN_APP`: Thông báo trong app

---

## API Endpoints

### 1. Lấy danh sách cài đặt thông báo

**Endpoint**: `GET /api/notification`

**Mô tả**: Lấy tất cả cài đặt thông báo của người dùng hiện tại.

**Quyền truy cập**: Authenticated (Bearer token)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "settingId": "setting-uuid-1",
      "notificationType": "CHARGING_COMPLETED",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-2",
      "notificationType": "CHARGING_COMPLETED",
      "channel": "PUSH",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-3",
      "notificationType": "PAYMENT_SUCCESS",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-4",
      "notificationType": "PAYMENT_SUCCESS",
      "channel": "SMS",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-5",
      "notificationType": "PROMOTION",
      "channel": "EMAIL",
      "isEnabled": false
    }
  ]
}
```

**Response Fields** (mỗi setting):
- `settingId` (string): ID của cài đặt
- `notificationType` (string): Loại thông báo
- `channel` (string): Kênh gửi thông báo (EMAIL | SMS | PUSH | IN_APP)
- `isEnabled` (boolean): Trạng thái bật/tắt

**Lưu ý**: Mỗi setting là một tổ hợp của (notificationType + channel). Ví dụ: "CHARGING_COMPLETED qua EMAIL" và "CHARGING_COMPLETED qua PUSH" là 2 settings riêng biệt.

---

### 2. Cập nhật nhiều cài đặt cùng lúc (Batch Update)

**Endpoint**: `PUT /api/notification`

**Mô tả**: Cập nhật nhiều cài đặt thông báo cùng một lúc.

**Quyền truy cập**: Authenticated (Bearer token)

**Request Body**:
```json
{
  "settings": [
    {
      "notificationType": "CHARGING_COMPLETED",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "notificationType": "CHARGING_COMPLETED",
      "channel": "PUSH",
      "isEnabled": true
    },
    {
      "notificationType": "PAYMENT_SUCCESS",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "notificationType": "PROMOTION",
      "channel": "EMAIL",
      "isEnabled": false
    }
  ]
}
```

**Request Fields**:
- `settings` (array, required): Danh sách các cài đặt cần cập nhật

**Mỗi item trong settings**:
- `notificationType` (string, required): Loại thông báo
- `channel` (string, required): Kênh gửi (EMAIL | SMS | PUSH | IN_APP)
- `isEnabled` (boolean, required): Bật/tắt

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "settingId": "setting-uuid-1",
      "notificationType": "CHARGING_COMPLETED",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-2",
      "notificationType": "CHARGING_COMPLETED",
      "channel": "PUSH",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-3",
      "notificationType": "PAYMENT_SUCCESS",
      "channel": "EMAIL",
      "isEnabled": true
    },
    {
      "settingId": "setting-uuid-4",
      "notificationType": "PROMOTION",
      "channel": "EMAIL",
      "isEnabled": false
    }
  ]
}
```

---

### 3. Cập nhật một cài đặt cụ thể

**Endpoint**: `PUT /api/notification/{settingId}`

**Mô tả**: Cập nhật một cài đặt thông báo cụ thể.

**Quyền truy cập**: Authenticated (Bearer token)

**Path Parameters**:
- `settingId` (string, required): ID của cài đặt cần cập nhật

**Request Body**:
```json
{
  "notificationType": "CHARGING_COMPLETED",
  "channel": "EMAIL",
  "isEnabled": true
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "settingId": "setting-uuid-1",
    "notificationType": "CHARGING_COMPLETED",
    "channel": "EMAIL",
    "isEnabled": true
  }
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
| 1006 | Not found | Không tìm thấy cài đặt |

---

## Lưu ý khi sử dụng

1. **Cấu trúc dữ liệu**: 
   - Mỗi setting là một tổ hợp của (notificationType + channel)
   - Người dùng có thể bật CHARGING_COMPLETED cho EMAIL nhưng tắt cho SMS

2. **Batch Update**:
   - Gửi tất cả settings cần thay đổi trong một request
   - Hệ thống tự động tạo mới hoặc cập nhật settings hiện có

3. **Default Settings**:
   - Khi tạo user mới, hệ thống có thể tự động tạo default settings
   - User có thể customize sau

4. **Notification Channels**:
   - **EMAIL**: Gửi qua email đã đăng ký
   - **SMS**: Gửi qua số điện thoại (cần phone number)
   - **PUSH**: Push notification trên mobile app
   - **IN_APP**: Hiển thị trong app (notification center)

5. **Best Practices**:
   - Cho phép user tắt PROMOTION nhưng nên giữ bật các thông báo quan trọng như PAYMENT_SUCCESS
   - Các thông báo hệ thống quan trọng (PAYMENT_FAILED) không nên cho phép tắt hoàn toàn
