# Plan & Subscription API Documentation

## Tổng quan

API quản lý gói dịch vụ (Plan) và đăng ký gói (Subscription) cho phép ADMIN tạo và quản lý các gói, còn DRIVER có thể đăng ký và quản lý subscription của mình.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token

---

## Enums

### BillingType
- `PAY_AS_YOU_GO`: Trả theo lượng sử dụng
- `MONTHLY_SUBSCRIPTION`: Đăng ký theo tháng
- `PREPAID`: Trả trước
- `POSTPAID`: Trả sau
- `VIP`: Gói VIP (yêu cầu phí tháng)

### SubscriptionStatus
- `ACTIVE`: Đang hoạt động
- `EXPIRED`: Đã hết hạn
- `CANCELLED`: Đã hủy

---

## PLAN MANAGEMENT APIs (ADMIN)

### 1. Tạo gói dịch vụ mới

**Endpoint**: `POST /api/plans`

**Mô tả**: ADMIN tạo gói dịch vụ mới với các cấu hình khác nhau theo billingType.

**Quyền truy cập**: ADMIN

**Request Body**:
```json
{
  "name": "Gói Premium",
  "billingType": "MONTHLY_SUBSCRIPTION",
  "monthlyFee": 500000.0,
  "discountPercent": 15.0,
  "freeChargingMinutes": 120,
  "description": "Gói Premium dành cho người dùng thường xuyên"
}
```

**Request Fields**:
- `name` (string, required): Tên gói (phải unique)
- `billingType` (string, required): Loại thanh toán (PAY_AS_YOU_GO | MONTHLY_SUBSCRIPTION | PREPAID | POSTPAID | VIP)
- `monthlyFee` (number, optional): Phí tháng (VNĐ) - Bắt buộc với VIP
- `discountPercent` (number, optional): Phần trăm giảm giá (0-100)
- `freeChargingMinutes` (integer, optional): Số phút sạc miễn phí mỗi tháng
- `description` (string, optional): Mô tả chi tiết gói

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "plan-uuid-1",
    "name": "Gói Premium",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "monthlyFee": 500000.0,
    "discountPercent": 15.0,
    "freeChargingMinutes": 120,
    "description": "Gói Premium dành cho người dùng thường xuyên"
  }
}
```

---

### 2. Lấy danh sách tất cả gói

**Endpoint**: `GET /api/plans`

**Mô tả**: Lấy danh sách tất cả các gói dịch vụ (public, để driver xem và chọn).

**Quyền truy cập**: Public

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "plan-uuid-1",
      "name": "Gói Free",
      "billingType": "PAY_AS_YOU_GO",
      "monthlyFee": 0.0,
      "discountPercent": 0.0,
      "freeChargingMinutes": 0,
      "description": "Gói miễn phí, trả theo lượng sử dụng"
    },
    {
      "id": "plan-uuid-2",
      "name": "Gói Premium",
      "billingType": "MONTHLY_SUBSCRIPTION",
      "monthlyFee": 500000.0,
      "discountPercent": 15.0,
      "freeChargingMinutes": 120,
      "description": "Gói Premium dành cho người dùng thường xuyên"
    },
    {
      "id": "plan-uuid-3",
      "name": "Gói VIP",
      "billingType": "VIP",
      "monthlyFee": 1000000.0,
      "discountPercent": 25.0,
      "freeChargingMinutes": 300,
      "description": "Gói VIP với nhiều ưu đãi nhất"
    }
  ]
}
```

---

### 3. Lấy chi tiết một gói

**Endpoint**: `GET /api/plans/{planId}`

**Mô tả**: Xem chi tiết một gói dịch vụ cụ thể.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `planId` (string, required): ID của gói

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "plan-uuid-2",
    "name": "Gói Premium",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "monthlyFee": 500000.0,
    "discountPercent": 15.0,
    "freeChargingMinutes": 120,
    "description": "Gói Premium dành cho người dùng thường xuyên"
  }
}
```

---

### 4. Cập nhật gói dịch vụ

**Endpoint**: `PUT /api/plans/{planId}`

**Mô tả**: ADMIN cập nhật thông tin gói dịch vụ.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `planId` (string, required): ID của gói

**Request Body**:
```json
{
  "name": "Gói Premium Plus",
  "monthlyFee": 600000.0,
  "discountPercent": 20.0,
  "freeChargingMinutes": 150,
  "description": "Gói Premium nâng cấp"
}
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "plan-uuid-2",
    "name": "Gói Premium Plus",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "monthlyFee": 600000.0,
    "discountPercent": 20.0,
    "freeChargingMinutes": 150,
    "description": "Gói Premium nâng cấp"
  }
}
```

---

### 5. Xóa gói dịch vụ

**Endpoint**: `DELETE /api/plans/{planId}`

**Mô tả**: ADMIN xóa gói dịch vụ khỏi hệ thống.

**Quyền truy cập**: ADMIN

**Path Parameters**:
- `planId` (string, required): ID của gói

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Plan deleted successfully"
}
```

**Lưu ý**: Không nên xóa gói đang có người đăng ký. Nên deactivate thay vì xóa.

---

## SUBSCRIPTION APIs (DRIVER)

### 6. Đăng ký gói dịch vụ

**Endpoint**: `POST /api/subscriptions`

**Mô tả**: Driver đăng ký một gói dịch vụ. Nếu gói có phí tháng, cần cung cấp phương thức thanh toán.

**Quyền truy cập**: DRIVER

**Request Body**:
```json
{
  "planId": "plan-uuid-2",
  "paymentMethodId": "pm-uuid-1",
  "autoRenew": true
}
```

**Request Fields**:
- `planId` (string, required): ID của gói muốn đăng ký
- `paymentMethodId` (string, optional): ID phương thức thanh toán (bắt buộc nếu gói có phí)
- `autoRenew` (boolean, optional): Tự động gia hạn khi hết hạn (mặc định: false)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "sub-uuid-1",
    "planId": "plan-uuid-2",
    "planName": "Gói Premium",
    "driverId": "driver-uuid-1",
    "startDate": "2025-10-01T00:00:00",
    "endDate": "2025-10-31T23:59:59",
    "status": "ACTIVE",
    "autoRenew": true,
    "monthlyFee": 500000.0,
    "discountPercent": 15.0,
    "freeChargingMinutes": 120,
    "remainingFreeMinutes": 120
  }
}
```

---

### 7. Xem subscription hiện tại

**Endpoint**: `GET /api/subscriptions/active`

**Mô tả**: Driver xem subscription đang hoạt động của mình.

**Quyền truy cập**: DRIVER

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "sub-uuid-1",
    "planId": "plan-uuid-2",
    "planName": "Gói Premium",
    "driverId": "driver-uuid-1",
    "startDate": "2025-10-01T00:00:00",
    "endDate": "2025-10-31T23:59:59",
    "status": "ACTIVE",
    "autoRenew": true,
    "monthlyFee": 500000.0,
    "discountPercent": 15.0,
    "freeChargingMinutes": 120,
    "remainingFreeMinutes": 85
  }
}
```

**Response khi không có subscription**:
```json
{
  "code": 1006,
  "message": "No active subscription found"
}
```

---

### 8. Hủy subscription

**Endpoint**: `DELETE /api/subscriptions/{subscriptionId}`

**Mô tả**: Driver hủy subscription hiện tại. Subscription sẽ vẫn có hiệu lực đến hết kỳ đã thanh toán.

**Quyền truy cập**: DRIVER

**Path Parameters**:
- `subscriptionId` (string, required): ID của subscription

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Subscription cancelled successfully"
}
```

**Lưu ý**: 
- Subscription sẽ chuyển sang trạng thái CANCELLED
- Vẫn được sử dụng đến hết ngày endDate
- Không được hoàn tiền

---

### 9. Bật/Tắt tự động gia hạn

**Endpoint**: `PATCH /api/subscriptions/{subscriptionId}/auto-renew`

**Mô tả**: Driver bật/tắt tính năng tự động gia hạn subscription.

**Quyền truy cập**: DRIVER

**Path Parameters**:
- `subscriptionId` (string, required): ID của subscription

**Query Parameters**:
- `autoRenew` (boolean, required): true để bật, false để tắt

**Example Request**:
```
PATCH /api/subscriptions/sub-uuid-1/auto-renew?autoRenew=true
```

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "sub-uuid-1",
    "planId": "plan-uuid-2",
    "planName": "Gói Premium",
    "driverId": "driver-uuid-1",
    "startDate": "2025-10-01T00:00:00",
    "endDate": "2025-10-31T23:59:59",
    "status": "ACTIVE",
    "autoRenew": true,
    "monthlyFee": 500000.0,
    "discountPercent": 15.0,
    "freeChargingMinutes": 120,
    "remainingFreeMinutes": 85
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ (tên gói trùng, config sai, v.v.) |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Not found | Không tìm thấy plan/subscription |
| 1008 | Payment method required | Gói có phí nhưng chưa cung cấp phương thức thanh toán |
| 1009 | Already subscribed | Đã có subscription đang hoạt động |

---

## Lưu ý

1. **Plan Configuration theo BillingType**:
   - **PAY_AS_YOU_GO**: Không yêu cầu monthlyFee, trả theo usage
   - **MONTHLY_SUBSCRIPTION**: Có monthlyFee, có thể có discount và free minutes
   - **VIP**: Bắt buộc có monthlyFee > 0, thường có discount cao nhất
   - **PREPAID/POSTPAID**: Tùy chỉnh theo nhu cầu

2. **Subscription Lifecycle**:
   - Khi đăng ký: status = ACTIVE, startDate = hôm nay, endDate = cuối tháng
   - Khi hết hạn: status = EXPIRED (nếu không auto-renew)
   - Khi hủy: status = CANCELLED (nhưng vẫn dùng được đến endDate)

3. **Auto-Renew**:
   - Nếu bật, hệ thống tự động gia hạn vào ngày endDate
   - Cần có payment method hợp lệ
   - Nếu thanh toán thất bại, subscription sẽ expire

4. **Free Charging Minutes**:
   - Reset vào đầu mỗi chu kỳ subscription
   - Sử dụng trước, trả tiền cho phần vượt quá
   - Không được chuyển sang tháng sau

5. **Discount Calculation**:
   - Discount áp dụng cho giá sạc (pricePerKwh)
   - Ví dụ: discount 15% → chỉ trả 85% giá gốc

6. **Switching Plans**:
   - Để đổi gói, cần hủy subscription hiện tại rồi đăng ký gói mới
   - Hoặc đợi subscription hiện tại hết hạn

