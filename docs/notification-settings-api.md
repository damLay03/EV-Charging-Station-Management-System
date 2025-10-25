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
      "id": "setting-uuid-1",
      "userId": "user-uuid-1",
      "notificationType": "CHARGING_COMPLETED",
      "emailEnabled": true,
      "smsEnabled": false,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "id": "setting-uuid-2",
      "userId": "user-uuid-1",
      "notificationType": "PAYMENT_SUCCESS",
      "emailEnabled": true,
      "smsEnabled": true,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "id": "setting-uuid-3",
      "userId": "user-uuid-1",
      "notificationType": "SUBSCRIPTION_EXPIRING",
      "emailEnabled": true,
      "smsEnabled": false,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "id": "setting-uuid-4",
      "userId": "user-uuid-1",
      "notificationType": "PROMOTION",
      "emailEnabled": false,
      "smsEnabled": false,
      "pushEnabled": false,
      "inAppEnabled": false
    }
  ]
}
```

**Response Fields** (mỗi setting):
- `id` (string): ID của cài đặt
- `userId` (string): ID người dùng
- `notificationType` (string): Loại thông báo
- `emailEnabled` (boolean): Bật/tắt email
- `smsEnabled` (boolean): Bật/tắt SMS
- `pushEnabled` (boolean): Bật/tắt push notification
- `inAppEnabled` (boolean): Bật/tắt in-app notification

---

### 2. Cập nhật nhiều cài đặt cùng lúc (Batch Update)

**Endpoint**: `PUT /api/notification`

**Mô tả**: Cập nhật nhiều cài đặt thông báo cùng một lúc (khi người dùng nhấn "Lưu cài đặt").

**Quyền truy cập**: Authenticated (Bearer token)

**Request Body**:
```json
{
  "settings": [
    {
      "notificationType": "CHARGING_COMPLETED",
      "emailEnabled": true,
      "smsEnabled": false,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "notificationType": "PAYMENT_SUCCESS",
      "emailEnabled": true,
      "smsEnabled": true,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "notificationType": "PROMOTION",
      "emailEnabled": false,
      "smsEnabled": false,
      "pushEnabled": false,
      "inAppEnabled": false
    }
  ]
}
```

**Request Fields**:
- `settings` (array, required): Danh sách các cài đặt cần cập nhật

**Mỗi item trong settings**:
- `notificationType` (string, required): Loại thông báo
- `emailEnabled` (boolean, required): Bật/tắt email
- `smsEnabled` (boolean, required): Bật/tắt SMS
- `pushEnabled` (boolean, required): Bật/tắt push
- `inAppEnabled` (boolean, required): Bật/tắt in-app

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "setting-uuid-1",
      "userId": "user-uuid-1",
      "notificationType": "CHARGING_COMPLETED",
      "emailEnabled": true,
      "smsEnabled": false,
      "pushEnabled": true,
      "inAppEnabled": true
    },
    {
      "id": "setting-uuid-2",
      "userId": "user-uuid-1",
      "notificationType": "PAYMENT_SUCCESS",
      "emailEnabled": true,
      "smsEnabled": true,
      "pushEnabled": true,
      "inAppEnabled": true
    }
  ]
}
```

---

### 3. Cập nhật một cài đặt cụ thể (Single Update)

**Endpoint**: `PATCH /api/notification/single`

**Mô tả**: Cập nhật một cài đặt cụ thể (khi người dùng toggle từng switch).

**Quyền truy cập**: Authenticated (Bearer token)

**Request Body**:
```json
{
  "notificationType": "PROMOTION",
  "emailEnabled": false,
  "smsEnabled": false,
  "pushEnabled": true,
  "inAppEnabled": false
}
```

**Request Fields**:
- `notificationType` (string, required): Loại thông báo cần cập nhật
- `emailEnabled` (boolean, optional): Bật/tắt email
- `smsEnabled` (boolean, optional): Bật/tắt SMS
- `pushEnabled` (boolean, optional): Bật/tắt push
- `inAppEnabled` (boolean, optional): Bật/tắt in-app

**Lưu ý**: Chỉ cần gửi các field muốn cập nhật, không cần gửi tất cả.

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "setting-uuid-4",
    "userId": "user-uuid-1",
    "notificationType": "PROMOTION",
    "emailEnabled": false,
    "smsEnabled": false,
    "pushEnabled": true,
    "inAppEnabled": false
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

---

## Lưu ý khi sử dụng

1. **Default Settings**:
   - Khi user mới đăng ký, hệ thống tự động tạo settings với giá trị mặc định
   - Mặc định bật email và push cho các thông báo quan trọng (CHARGING_COMPLETED, PAYMENT_SUCCESS)
   - Mặc định tắt SMS (do có phí) và PROMOTION

2. **Batch vs Single Update**:
   - **Batch Update** (`PUT /api/notification`): Dùng khi lưu toàn bộ form settings
   - **Single Update** (`PATCH /api/notification/single`): Dùng khi toggle từng switch riêng lẻ
   - Single update nhanh hơn, phù hợp với UX real-time

3. **Notification Types Priority**:
   - **Critical** (nên bật): CHARGING_COMPLETED, PAYMENT_SUCCESS, PAYMENT_FAILED
   - **Important**: CHARGING_STARTED, SUBSCRIPTION_EXPIRING, STATION_MAINTENANCE
   - **Optional**: PROMOTION, LOW_BATTERY

4. **Channel Costs**:
   - EMAIL: Miễn phí
   - PUSH/IN_APP: Miễn phí
   - SMS: Có phí, cân nhắc trước khi bật

5. **UI/UX Recommendations**:
   - Hiển thị settings dạng bảng với toggle switches
   - Mỗi hàng là một notification type
   - Mỗi cột là một channel (Email, SMS, Push, In-App)
   - Auto-save khi toggle hoặc có nút "Save All"

6. **Notification Delivery**:
   - Hệ thống check settings trước khi gửi notification
   - Chỉ gửi qua các channel được bật
   - Log lại delivery status để tracking

7. **Privacy**:
   - User có quyền tắt tất cả notification nếu muốn
   - Không bắt buộc phải bật bất kỳ channel nào
   - Tuy nhiên nên khuyến nghị bật ít nhất email cho thông báo quan trọng

8. **Validation**:
   - NotificationType phải hợp lệ (theo enum)
   - Ít nhất phải có một field enabled trong request
   - Không thể tạo duplicate settings cho cùng một notification type

## UI Example

Gợi ý layout cho trang Settings:

```
╔════════════════════════════════════════════════════════════╗
║  Notification Settings                                      ║
╠════════════════════════════════════════════════════════════╣
║  Notification Type          Email   SMS   Push   In-App    ║
╠────────────────────────────────────────────────────────────╣
║  Charging Completed          [✓]    [ ]   [✓]    [✓]      ║
║  Payment Success             [✓]    [✓]   [✓]    [✓]      ║
║  Subscription Expiring       [✓]    [ ]   [✓]    [✓]      ║
║  Promotion                   [ ]    [ ]   [ ]    [ ]      ║
║  ...                                                        ║
╠────────────────────────────────────────────────────────────╣
║                                        [Cancel]  [Save All] ║
╚════════════════════════════════════════════════════════════╝
```

