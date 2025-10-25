# VNPay Payment API Documentation

## Tổng quan

API thanh toán VNPay cho phép Driver thanh toán các phiên sạc đã hoàn thành thông qua cổng thanh toán VNPay.

- **Base URL**: `http://localhost:8084/evchargingstation`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: DRIVER (cho create payment), Public (cho callback)

---

## Flow thanh toán

1. **Driver request payment**: Frontend gọi `POST /api/payment/vnpay/create` với sessionId
2. **Backend tạo payment URL**: Backend tạo URL thanh toán VNPay và trả về cho frontend
3. **Redirect đến VNPay**: Frontend redirect user đến URL VNPay để thanh toán
4. **User thanh toán**: User nhập thông tin thẻ/ngân hàng và xác nhận thanh toán trên trang VNPay
5. **VNPay callback**: Sau khi thanh toán, VNPay redirect về `vnpay-callback` endpoint
6. **Backend xử lý**: Backend verify signature, cập nhật payment status
7. **Frontend hiển thị kết quả**: Frontend hiển thị kết quả thanh toán cho user

---

## API Endpoints

### 1. Tạo Payment URL

**Endpoint**: `POST /api/payment/vnpay/create`

**Mô tả**: Tạo URL thanh toán VNPay cho charging session đã hoàn thành.

**Quyền truy cập**: DRIVER (Bearer token)

**Request Body**:
```json
{
  "sessionId": "session-uuid-123",
  "bankCode": "NCB"
}
```

**Request Fields**:
- `sessionId` (string, required): ID của charging session cần thanh toán
- `bankCode` (string, optional): Mã ngân hàng/phương thức thanh toán
  - `NCB`: Ngân hàng NCB
  - `VNPAYQR`: Quét mã QR VNPay
  - `VNBANK`: Thẻ ATM nội địa
  - `INTCARD`: Thẻ quốc tế (Visa, MasterCard)
  - Để trống: VNPay sẽ hiển thị tất cả phương thức

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "code": "00",
    "message": "success",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000000&vnp_Command=pay&..."
  }
}
```

**Response Fields**:
- `code` (string): Mã trạng thái ("00" = success)
- `message` (string): Thông báo
- `paymentUrl` (string): URL để redirect user đến trang thanh toán VNPay

**Error Responses**:

```json
{
  "code": 15003,
  "message": "Charging Session Not Found"
}
```

```json
{
  "code": 16001,
  "message": "Charging Session Not Completed Yet"
}
```

```json
{
  "code": 16002,
  "message": "Payment Already Completed"
}
```

**Cách sử dụng**:
1. Frontend gọi API này để lấy `paymentUrl`
2. Frontend redirect user đến `paymentUrl` bằng `window.location.href = paymentUrl`
3. User sẽ thấy trang thanh toán VNPay

---

### 2. VNPay Callback (IPN)

**Endpoint**: `GET /api/payment/vnpay-callback`

**Mô tả**: Endpoint nhận callback từ VNPay sau khi user thanh toán. VNPay sẽ tự động gọi endpoint này.

**Quyền truy cập**: Public (VNPay gọi trực tiếp, không cần Bearer token)

**Query Parameters** (VNPay tự động gửi):
- `vnp_ResponseCode`: Mã phản hồi ("00" = thành công)
- `vnp_TxnRef`: Mã giao dịch (sessionId)
- `vnp_TransactionNo`: Mã giao dịch VNPay
- `vnp_Amount`: Số tiền (đã nhân 100)
- `vnp_BankCode`: Mã ngân hàng
- `vnp_CardType`: Loại thẻ (ATM, QRCODE, etc.)
- `vnp_PayDate`: Ngày thanh toán (yyyyMMddHHmmss)
- `vnp_SecureHash`: Chữ ký bảo mật

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "code": "00",
    "message": "Payment successful",
    "sessionId": "session-uuid-123",
    "transactionId": "14012025",
    "amount": 100000,
    "paymentStatus": "SUCCESS",
    "bankCode": "NCB",
    "cardType": "ATM",
    "payDate": "20251026143000"
  }
}
```

**Response Fields**:
- `code` (string): VNPay response code
  - `00`: Giao dịch thành công
  - `07`: Trừ tiền thành công nhưng giao dịch nghi vấn
  - `09`: Giao dịch không thành công do thẻ chưa đăng ký dịch vụ
  - `10`: Giao dịch không thành công do sai thông tin
  - `11`: Giao dịch không thành công do hết hạn
  - `12`: Giao dịch không thành công do thẻ bị khóa
  - `24`: Giao dịch không thành công do hủy
  - `51`: Giao dịch không thành công do tài khoản không đủ số dư
  - `65`: Giao dịch không thành công do vượt quá số lần nhập sai OTP
- `message` (string): Thông báo kết quả
- `sessionId` (string): ID của charging session
- `transactionId` (string): Mã giao dịch VNPay
- `amount` (number): Số tiền đã thanh toán (VNĐ)
- `paymentStatus` (string): SUCCESS hoặc FAILED
- `bankCode` (string): Mã ngân hàng
- `cardType` (string): Loại thẻ
- `payDate` (string): Ngày thanh toán

**Lưu ý**:
- Frontend KHÔNG gọi endpoint này trực tiếp
- VNPay sẽ redirect user về URL này sau khi thanh toán
- Frontend cần xử lý query parameters từ URL để hiển thị kết quả

---

## Cách Frontend xử lý

### Bước 1: Tạo payment và redirect

```javascript
// 1. Gọi API tạo payment URL
const response = await fetch('/api/payment/vnpay/create', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    sessionId: 'session-uuid-123',
    bankCode: 'NCB' // optional
  })
});

const data = await response.json();

// 2. Redirect user đến VNPay
if (data.code === 1000) {
  window.location.href = data.result.paymentUrl;
}
```

### Bước 2: Xử lý callback trên trang payment-result

Tạo trang `/payment-result` để VNPay redirect về:

```javascript
// payment-result.jsx hoặc payment-result.html

// 1. Lấy query parameters từ URL
const urlParams = new URLSearchParams(window.location.search);
const responseCode = urlParams.get('vnp_ResponseCode');
const sessionId = urlParams.get('vnp_TxnRef');
const amount = parseInt(urlParams.get('vnp_Amount')) / 100;

// 2. Hiển thị kết quả
if (responseCode === '00') {
  // Thanh toán thành công
  showSuccess(`Thanh toán ${amount.toLocaleString()}đ thành công!`);
  // Redirect về trang charging history
  setTimeout(() => {
    window.location.href = '/charging-history';
  }, 3000);
} else {
  // Thanh toán thất bại
  showError('Thanh toán thất bại. Vui lòng thử lại!');
}
```

---

## Thông tin test VNPay Sandbox

### Thẻ ATM nội địa (NCB)
- **Số thẻ**: 9704198526191432198
- **Tên chủ thẻ**: NGUYEN VAN A
- **Ngày phát hành**: 07/15
- **Mật khẩu OTP**: 123456

### Thẻ quốc tế (INTCARD)
- **Số thẻ**: 4111111111111111
- **Tên chủ thẻ**: NGUYEN VAN A
- **Ngày hết hạn**: 12/25
- **CVV**: 123

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 15003 | Charging Session Not Found | Không tìm thấy phiên sạc |
| 16001 | Charging Session Not Completed Yet | Phiên sạc chưa hoàn thành |
| 16002 | Payment Already Completed | Đã thanh toán rồi |
| 16003 | Invalid Payment Signature | Chữ ký không hợp lệ |
| 16004 | Payment Processing Failed | Xử lý thanh toán thất bại |

---

## Lưu ý quan trọng

1. **Return URL**: 
   - Development: `http://localhost:8084/evchargingstation/api/payment/vnpay-callback`
   - Production: Thay đổi trong `application.yaml` thành domain thực

2. **Amount**: VNPay yêu cầu số tiền nhân 100 (100,000 VNĐ → 10,000,000)

3. **Security**:
   - Luôn verify `vnp_SecureHash` trong callback
   - Không tin tưởng dữ liệu từ query parameters mà không verify

4. **Transaction ID**: Sử dụng `sessionId` làm `vnp_TxnRef` để tracking

5. **Payment Status**: Kiểm tra cả `vnp_ResponseCode` và payment status trong database

6. **Timeout**: Payment URL có hiệu lực 15 phút (cấu hình trong VNPAYConfig)

7. **Testing**: Sử dụng sandbox environment và thẻ test của VNPay

---

## Flow diagram

```
Driver                  Frontend                Backend                VNPay
  |                        |                       |                      |
  |--Click "Thanh toán"--> |                       |                      |
  |                        |--POST /vnpay/create-->|                      |
  |                        |                       |--Tạo payment URL---> |
  |                        |<--Return payment URL--|                      |
  |<--Redirect to VNPay----|                       |                      |
  |                        |                       |                      |
  |-------Nhập thông tin thẻ, xác nhận OTP-------->|                      |
  |                        |                       |                      |
  |                        |<--------Redirect (callback)------------------|
  |                        |                       |<--Verify & Update----|
  |<--Hiển thị kết quả-----|                       |                      |
```

---

## Production Checklist

- [ ] Thay `vnp_TmnCode` và `vnp_HashSecret` thành thông tin production
- [ ] Cập nhật `vnpay.url` thành production URL
- [ ] Cập nhật `vnpay.returnUrl` thành domain thực của bạn
- [ ] Test thoroughly với môi trường production
- [ ] Setup logging để track payment transactions
- [ ] Implement payment reconciliation

