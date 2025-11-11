# API Hủy Gia Hạn Gói Cước (Auto-Renewal Management)

## Tổng Quan
Tính năng cho phép driver **hủy gia hạn tự động** gói cước. Khi hủy:
- Gói hiện tại vẫn **dùng được đến hết tháng**
- Sau đó tự động chuyển về gói **"Linh hoạt"** (default plan)
- Không bị trừ tiền gia hạn tự động

## Endpoints

### 1. Hủy Gia Hạn Tự Động
**POST** `/api/plans/cancel-auto-renewal`

**Authorization:** Bearer Token (DRIVER role)

**Response Success (200):**
```json
{
  "code": 1000,
  "message": "Auto-renewal canceled successfully. Your plan will remain active until the end of the period.",
  "result": {
    "planId": "abc123",
    "name": "Tiết kiệm",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 3200,
    "pricePerMinute": 0,
    "monthlyFee": 50000,
    "benefits": "Giảm giá 15% cho tất cả các phiên sạc"
  }
}
```

**Luồng hoạt động:**
1. Driver gọi API hủy gia hạn
2. Hệ thống set `planAutoRenew = false`
3. Gửi email thông báo hủy thành công
4. Gói vẫn hoạt động đến hết tháng
5. Khi đến ngày gia hạn (sau 30 ngày), hệ thống kiểm tra `planAutoRenew`
6. Nếu `false` → Tự động chuyển về gói "Linh hoạt"

### 2. Kích Hoạt Lại Gia Hạn Tự Động
**POST** `/api/plans/reactivate-auto-renewal`

**Authorization:** Bearer Token (DRIVER role)

**Response Success (200):**
```json
{
  "code": 1000,
  "message": "Auto-renewal reactivated successfully. Your plan will auto-renew next month.",
  "result": {
    "planId": "abc123",
    "name": "Tiết kiệm",
    "billingType": "MONTHLY_SUBSCRIPTION",
    "pricePerKwh": 3200,
    "pricePerMinute": 0,
    "monthlyFee": 50000,
    "benefits": "Giảm giá 15% cho tất cả các phiên sạc"
  }
}
```

**Luồng hoạt động:**
1. Driver gọi API kích hoạt lại
2. Hệ thống set `planAutoRenew = true`
3. Gửi email thông báo kích hoạt thành công
4. Gói sẽ tự động gia hạn vào tháng sau

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 6001 | Plan Not Found | Driver không có gói hiện tại hoặc gói không phải MONTHLY/VIP |
| 1005 | Unauthenticated | Token không hợp lệ |

## Email Notifications

### 1. Email Hủy Gia Hạn
**Subject:** 🔔 Hủy gia hạn tự động thành công

**Nội dung:**
- Gói hiện tại vẫn hoạt động đến [ngày hết hạn]
- Sau khi hết hạn → Tự động chuyển về "Linh hoạt"
- Có thể kích hoạt lại bất kỳ lúc nào

### 2. Email Kích Hoạt Lại
**Subject:** ✅ Kích hoạt lại gia hạn tự động

**Nội dung:**
- Xác nhận bật lại gia hạn tự động
- Nhắc nhở đảm bảo ví có đủ tiền

### 3. Email Chuyển Về Linh Hoạt
**Subject:** 📢 Gói cước đã hết hạn

**Nội dung:**
- Thông báo gói cũ đã hết hạn
- Đã tự động chuyển về "Linh hoạt"
- Có thể đăng ký lại bất kỳ lúc nào

## Database Changes

Đã thêm cột mới vào bảng `drivers`:

```sql
ALTER TABLE drivers 
ADD COLUMN plan_auto_renew BOOLEAN DEFAULT TRUE 
COMMENT 'Trạng thái tự động gia hạn gói (true = bật, false = tắt)';
```

**Chạy migration:**
```bash
# File migration đã được tạo tại:
docs/database-migration-plan-auto-renew.sql
```

## Ví Dụ Sử Dụng

### Scenario 1: Hủy gia hạn
```bash
# 1. Driver đang dùng gói "Tiết kiệm" (50k/tháng)
# Đăng ký ngày: 01/11/2025

# 2. Ngày 15/11, driver muốn hủy gia hạn
curl -X POST https://api.example.com/api/plans/cancel-auto-renewal \
  -H "Authorization: Bearer <token>"

# 3. Gói vẫn hoạt động đến 01/12/2025
# 4. Từ 01/12, tự động chuyển về "Linh hoạt"
```

### Scenario 2: Đổi ý, kích hoạt lại
```bash
# 1. Ngày 20/11, driver đổi ý muốn tiếp tục dùng gói
curl -X POST https://api.example.com/api/plans/reactivate-auto-renewal \
  -H "Authorization: Bearer <token>"

# 2. Gói sẽ tự động gia hạn vào 01/12 (nếu ví đủ tiền)
```

## Notes

- ⚠️ Chỉ áp dụng cho gói **MONTHLY_SUBSCRIPTION** và **VIP**
- ⚠️ Gói **PAY_AS_YOU_GO** (Linh hoạt) không có tính năng này
- ✅ Có thể hủy và kích hoạt lại nhiều lần trước khi hết hạn
- ✅ Không mất phí khi hủy hoặc kích hoạt lại

