# 🔧 Fix ZaloPay Callback 401 Error

## ❌ Vấn đề

Lỗi **401 Unauthorized** khi ZaloPay gọi callback về server:
```
20:22:09.180 +07 POST /evchargingstation/api/payment/zalopay-callback 401
```

## 🔍 Nguyên nhân

**URL không khớp** giữa các nơi:

| File | URL cũ (SAI) | URL mới (ĐÚNG) |
|------|--------------|----------------|
| SecurityConfig.java | `/api/payments/callbacks/zalopay` | `/api/webhooks/zalopay/callback` |
| application.yaml | `/api/payment/zalopay-callback` | `/api/webhooks/zalopay/callback` |
| WebhookController.java | - | `/api/webhooks/zalopay/callback` ✅ |

→ Spring Security chặn vì URL không nằm trong danh sách PUBLIC_ENDPOINTS

## ✅ Đã fix

### 1. SecurityConfig.java
```java
private static final String[] PUBLIC_ENDPOINTS = {
    // ...
    "/api/webhooks/zalopay/callback",  // ✅ Fixed
    // ...
};
```

### 2. application.yaml
```yaml
zalopay:
  callback-url: "https://unendued-somnolent-rosemarie.ngrok-free.dev/evchargingstation/api/webhooks/zalopay/callback"  # ✅ Fixed
```

## 🧪 Test với Postman

### 1. Test Endpoint Public (không cần auth)

**Request:**
```
POST http://localhost:8080/evchargingstation/api/webhooks/zalopay/callback
Content-Type: application/json

{
  "data": "{\"app_id\":2554,\"app_trans_id\":\"250108_test123\",\"app_time\":1699459329180,\"amount\":50000,\"embed_data\":\"{}\",\"item\":\"[]\",\"zp_trans_id\":240001234567,\"server_time\":1699459330000,\"channel\":1,\"merchant_user_id\":\"user123\",\"user_fee_amount\":0,\"discount_amount\":0}",
  "mac": "test_mac_string"
}
```

**Expected Response:**
- Status: **200 OK** (không phải 401)
- Body:
```json
{
  "return_code": -1,
  "return_message": "Invalid MAC"  // Vì MAC test không đúng
}
```

hoặc nếu có payment tương ứng:
```json
{
  "return_code": 1,
  "return_message": "Success"
}
```

### 2. Verify Log

Check console log khi gọi API:
```
=== ZaloPay Webhook Callback Received ===
Data: {...}
MAC: test_mac_string
Received ZaloPay callback
Invalid callback MAC  // hoặc Processing callback for transaction...
Webhook response: {return_code=-1, return_message=Invalid MAC}
=== End ZaloPay Webhook Callback ===
```

### 3. Test với ZaloPay thực tế

Sau khi deploy lên ngrok/server:

**A. Tạo payment:**
```bash
POST http://localhost:8080/evchargingstation/api/payments/zalopay
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 50000
}
```

**B. Thanh toán qua ZaloPay app:**
- Scan QR code hoặc mở order_url
- Thanh toán thành công

**C. ZaloPay sẽ tự động gọi callback:**
```
POST https://your-ngrok.ngrok-free.dev/evchargingstation/api/webhooks/zalopay/callback
```

**D. Verify payment status:**
```bash
GET http://localhost:8080/evchargingstation/api/payments/{paymentId}
```

Expected:
```json
{
  "status": "COMPLETED",  // ✅ Đã được cập nhật từ callback
  "paidAt": "2025-11-08T20:22:09"
}
```

## 🔐 Security Note

Endpoint callback **PHẢI** public vì:
- ZaloPay server gọi từ bên ngoài (không có JWT token)
- Nhưng có bảo mật bằng **MAC verification**:
  ```java
  boolean isValid = ZaloPayUtil.verifyCallbackMac(
      callbackRequest.getData(),
      callbackRequest.getMac(),
      zaloPayConfig.getKey2()
  );
  ```

## 📝 Checklist

- [x] Fix SecurityConfig.java - thêm `/api/webhooks/zalopay/callback` vào PUBLIC_ENDPOINTS
- [x] Fix application.yaml - sửa callback-url
- [ ] Restart application
- [ ] Test với Postman → expect 200 (không phải 401)
- [ ] Test full flow: create payment → pay → verify callback

## 🚀 Deploy Steps

1. **Restart app** để load config mới:
   ```bash
   mvn spring-boot:run
   ```

2. **Update ngrok URL** nếu đổi (hoặc dùng paid plan để fixed domain):
   ```bash
   ngrok http 8080
   # Copy HTTPS URL và update vào application.yaml
   ```

3. **Test ngay:**
   ```bash
   curl -X POST http://localhost:8080/evchargingstation/api/webhooks/zalopay/callback \
     -H "Content-Type: application/json" \
     -d '{"data":"test","mac":"test"}'
   ```
   
   Expect: 200 OK (không phải 401 ❌)

## 🐛 Troubleshooting

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| 401 Unauthorized | URL không public | Check SecurityConfig |
| 404 Not Found | URL sai | Check WebhookController mapping |
| Invalid MAC | MAC verification fail | Check key2 config hoặc data format |
| Payment not found | Transaction ID không tồn tại | Check database, create payment trước |

---

**✅ Sau khi fix, ZaloPay callback sẽ work bình thường!**

