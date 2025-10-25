# Plan Management API Documentation

## Tổng quan

API quản lý gói dịch vụ (Plan) cho phép ADMIN tạo và quản lý các gói giá sạc điện khác nhau.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: ADMIN (tạo/sửa/xóa), Public (xem danh sách)

---

## Enums

### BillingType
- `PAY_AS_YOU_GO`: Trả theo lượng sử dụng
- `MONTHLY_SUBSCRIPTION`: Đăng ký theo tháng (có phí cố định)
- `VIP`: Gói VIP (yêu cầu phí tháng cao)

---

## API Endpoints

### 1. Tạo gói dịch vụ mới

**Endpoint**: `POST /api/plans`

**Mô tả**: ADMIN tạo gói dịch vụ mới với các cấu hình khác nhau theo billingType.

**Quyền truy cập**: ADMIN

**Request Body**:
```json
{
  "name": "Gói Tiêu chuẩn",
  "billingType": "PAY_AS_YOU_GO",
  "pricePerKwh": 5000.0,
  "pricePerMinute": 500.0,
  "monthlyFee": 0.0,
  "benefits": "Trả theo lượng sử dụng thực tế"
}
```

**Request Fields**:
- `name` (string, required): Tên gói (phải unique)
- `billingType` (string, required): Loại thanh toán (PAY_AS_YOU_GO | MONTHLY_SUBSCRIPTION | VIP)
- `pricePerKwh` (number, required): Giá mỗi kWh (VNĐ)
- `pricePerMinute` (number, optional): Giá mỗi phút (VNĐ), mặc định 0
- `monthlyFee` (number, optional): Phí tháng (VNĐ), bắt buộc > 0 với MONTHLY_SUBSCRIPTION và VIP
- `benefits` (string, optional): Mô tả các lợi ích của gói

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "planId": "plan-uuid-1",
    "name": "Gói Tiêu chuẩn",
    "billingType": "PAY_AS_YOU_GO",
    "pricePerKwh": 5000.0,
    "pricePerMinute": 500.0,
    "monthlyFee": 0.0,
    "benefits": "Trả theo lượng sử dụng thực tế"
  }
}
```

**Validation Rules**:
- `PAY_AS_YOU_GO`: monthlyFee phải = 0
- `MONTHLY_SUBSCRIPTION`: monthlyFee phải > 0
- `VIP`: monthlyFee phải > 0

**Error Response** (400 Bad Request):
```json
{
  "code": 6002,
  "message": "Plan Name Already Exists"
}
```

---

### 2. Lấy danh sách tất cả gói

**Endpoint**: `GET /api/plans`

**Mô tả**: Lấy danh sách tất cả các gói dịch vụ (public, không cần authentication).

**Quyền truy cập**: Public

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "planId": "plan-uuid-1",
      "name": "Gói Linh hoạt",
      "billingType": "PAY_AS_YOU_GO",
      "pricePerKwh": 5000.0,
      "pricePerMinute": 500.0,
      "monthlyFee": 0.0,
      "benefits": "Trả theo lượng sử dụng, không cam kết"
    },
    {
      "planId": "plan-uuid-2",
      "name": "Gói Premium",
      "billingType": "MONTHLY_SUBSCRIPTION",
      "pricePerKwh": 4500.0,
      "pricePerMinute": 400.0,
      "monthlyFee": 500000.0,
      "benefits": "Giảm 10% giá sạc, phí tháng 500k"
    },
    {
      "planId": "plan-uuid-3",
      "name": "Gói VIP",
      "billingType": "VIP",
      "pricePerKwh": 4000.0,
      "pricePerMinute": 300.0,
      "monthlyFee": 1000000.0,
      "benefits": "Giảm 20% giá sạc, ưu tiên đặt trước, hỗ trợ 24/7"
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
    "planId": "plan-uuid-2",
    "name": "Gói Premium",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 4500.0,
    "pricePerMinute": 400.0,
    "monthlyFee": 500000.0,
    "benefits": "Giảm 10% giá sạc, phí tháng 500k"
  }
}
```

**Error Response** (404 Not Found):
```json
{
  "code": 6001,
  "message": "Plan Not Found"
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
  "billingType": "MONTHLY_SUBSCRIPTION",
  "pricePerKwh": 4200.0,
  "pricePerMinute": 350.0,
  "monthlyFee": 600000.0,
  "benefits": "Giảm 16% giá sạc, phí tháng 600k"
}
```

**Request Fields**: Giống như Create Plan

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "planId": "plan-uuid-2",
    "name": "Gói Premium Plus",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 4200.0,
    "pricePerMinute": 350.0,
    "monthlyFee": 600000.0,
    "benefits": "Giảm 16% giá sạc, phí tháng 600k"
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "code": 6003,
  "message": "Invalid Plan Configuration"
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

**Error Response** (404 Not Found):
```json
{
  "code": 6001,
  "message": "Plan Not Found"
}
```

**Lưu ý**: Nếu gói đang được sử dụng bởi driver, không nên xóa. Khuyến nghị deactivate thay vì xóa.

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ |
| 1005 | Unauthorized | Không có quyền truy cập (không phải ADMIN) |
| 6001 | Plan not found | Không tìm thấy gói |
| 6002 | Plan name existed | Tên gói đã tồn tại |
| 6003 | Invalid plan config | Cấu hình gói không hợp lệ |

---

## Lưu ý khi sử dụng

1. **Plan Configuration theo BillingType**:
   - **PAY_AS_YOU_GO**: 
     - Không có phí tháng (monthlyFee = 0)
     - Trả theo usage thực tế (pricePerKwh, pricePerMinute)
     - Phù hợp cho người dùng không thường xuyên
   
   - **MONTHLY_SUBSCRIPTION**: 
     - Có phí tháng cố định (monthlyFee > 0)
     - Giá sạc thấp hơn PAY_AS_YOU_GO
     - Phù hợp cho người dùng thường xuyên
   
   - **VIP**: 
     - Phí tháng cao nhất (monthlyFee > 0)
     - Giá sạc ưu đãi nhất
     - Thêm các lợi ích đặc biệt (ưu tiên, hỗ trợ 24/7)

2. **Name Uniqueness**:
   - Tên gói phải unique trong hệ thống (case-insensitive)
   - Khi update, không được trùng với gói khác

3. **Pricing Strategy**:
   - `pricePerKwh`: Giá chính, tính theo năng lượng tiêu thụ
   - `pricePerMinute`: Giá phụ, tính theo thời gian (có thể = 0)
   - `monthlyFee`: Phí cố định hàng tháng

4. **Default Plan**:
   - Nên có ít nhất một plan "Linh hoạt" (PAY_AS_YOU_GO) làm mặc định
   - Plan này được dùng khi driver chưa chọn plan cụ thể

5. **Business Logic**:
   - Giá sạc của VIP < Premium < Pay As You Go
   - Phí tháng của VIP > Premium > Pay As You Go (= 0)

6. **Validation**:
   - ADMIN tạo plan với config phù hợp billing type
   - System validate tự động theo rules

## Ví dụ Pricing

### Gói Linh hoạt (PAY_AS_YOU_GO)
- Phí tháng: 0 VNĐ
- Giá sạc: 5,000 VNĐ/kWh + 500 VNĐ/phút
- Ví dụ: Sạc 20 kWh trong 60 phút = 100,000 + 30,000 = **130,000 VNĐ**

### Gói Premium (MONTHLY_SUBSCRIPTION)
- Phí tháng: 500,000 VNĐ
- Giá sạc: 4,500 VNĐ/kWh + 400 VNĐ/phút (giảm 10%)
- Ví dụ: Sạc 20 kWh trong 60 phút = 90,000 + 24,000 = **114,000 VNĐ**
- Tổng/tháng (sạc 10 lần): 500,000 + (114,000 × 10) = **1,640,000 VNĐ**

### Gói VIP
- Phí tháng: 1,000,000 VNĐ
- Giá sạc: 4,000 VNĐ/kWh + 300 VNĐ/phút (giảm 20%)
- Ví dụ: Sạc 20 kWh trong 60 phút = 80,000 + 18,000 = **98,000 VNĐ**
- Tổng/tháng (sạc 15 lần): 1,000,000 + (98,000 × 15) = **2,470,000 VNĐ**

---

## UI Recommendations

### Admin - Plan Management
```
╔═══════════════════════════════════════════════════════════╗
║  Plan Management                            [+ New Plan]  ║
╠═══════════════════════════════════════════════════════════╣
║  Name          Type              Price/kWh  Monthly Fee   ║
╠───────────────────────────────────────────────────────────╣
║  Linh hoạt     PAY_AS_YOU_GO     5,000      0            ║
║  Premium       MONTHLY_SUB       4,500      500,000      ║
║  VIP           VIP               4,000      1,000,000    ║
╚═══════════════════════════════════════════════════════════╝
```

### User - Plan Selection
Hiển thị plans dưới dạng cards với:
- Tên gói và loại
- Giá sạc (VNĐ/kWh)
- Phí tháng (nếu có)
- Danh sách lợi ích
- Nút "Chọn gói" hoặc "Đang dùng"

