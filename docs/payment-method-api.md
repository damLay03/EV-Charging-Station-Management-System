# Payment Method API Documentation

## Tổng quan

API quản lý phương thức thanh toán cho phép DRIVER thêm, xem và xóa các phương thức thanh toán của mình.

- **Base URL**: `http://localhost:8080`
- **Authentication**: Bearer JWT token
- **Quyền truy cập**: DRIVER

---

## Enums

### PaymentMethodType
- `CREDIT_CARD`: Thẻ tín dụng
- `DEBIT_CARD`: Thẻ ghi nợ
- `E_WALLET`: Ví điện tử (Momo, ZaloPay, VNPay, etc.)

---

## API Endpoints

### 1. Thêm phương thức thanh toán

**Endpoint**: `POST /api/payment-methods`

**Mô tả**: Driver thêm phương thức thanh toán mới vào tài khoản.

**Quyền truy cập**: DRIVER

**Request Body**:
```json
{
  "methodType": "CREDIT_CARD",
  "provider": "Visa",
  "token": "4111111111111111"
}
```

**Request Fields**:
- `methodType` (string, required): Loại thanh toán (CREDIT_CARD | DEBIT_CARD | E_WALLET)
- `provider` (string, optional): Nhà cung cấp (Visa, MasterCard, Momo, ZaloPay, VNPay)
- `token` (string, optional): Số thẻ/tài khoản (nên được mã hóa từ client)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "pmId": "pm-uuid-1",
    "methodType": "CREDIT_CARD",
    "provider": "Visa",
    "maskedToken": "************1111"
  }
}
```

**Response Fields**:
- `pmId` (string): ID của phương thức thanh toán
- `methodType` (string): Loại thanh toán
- `provider` (string, nullable): Nhà cung cấp
- `maskedToken` (string, nullable): Token đã được mask (chỉ hiển thị 4 số cuối)

**Lưu ý**: Số thẻ được mask (chỉ hiển thị 4 số cuối) vì bảo mật.

---

### 2. Xem danh sách phương thức thanh toán

**Endpoint**: `GET /api/payment-methods`

**Mô tả**: Driver xem tất cả phương thức thanh toán của mình.

**Quyền truy cập**: DRIVER

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "pmId": "pm-uuid-1",
      "methodType": "CREDIT_CARD",
      "provider": "Visa",
      "maskedToken": "************1111"
    },
    {
      "pmId": "pm-uuid-2",
      "methodType": "E_WALLET",
      "provider": "Momo",
      "maskedToken": "****4567"
    },
    {
      "pmId": "pm-uuid-3",
      "methodType": "DEBIT_CARD",
      "provider": "Vietcombank",
      "maskedToken": "************7890"
    }
  ]
}
```

---

### 3. Xóa phương thức thanh toán

**Endpoint**: `DELETE /api/payment-methods/{pmId}`

**Mô tả**: Driver xóa một phương thức thanh toán.

**Quyền truy cập**: DRIVER

**Path Parameters**:
- `pmId` (string, required): ID của phương thức thanh toán

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "message": "Payment method deleted successfully"
}
```

**Error Response** (400 Bad Request):
```json
{
  "code": 1002,
  "message": "Cannot delete payment method that is in use"
}
```

**Lưu ý**: Không thể xóa phương thức thanh toán đang được sử dụng cho các giao dịch đang xử lý.

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request thành công |
| 1002 | Invalid data | Dữ liệu không hợp lệ hoặc phương thức đang được sử dụng |
| 1004 | Unauthenticated | Chưa đăng nhập |
| 1005 | Unauthorized | Không có quyền truy cập |
| 1006 | Not found | Không tìm thấy phương thức thanh toán |

---

## Lưu ý khi sử dụng

1. **Bảo mật thông tin thẻ**:
   - Số thẻ được mã hóa và mask khi trả về API
   - Chỉ hiển thị 4 số cuối
   - Không bao giờ trả về CVV/CVC

2. **Validation**:
   - Số thẻ phải hợp lệ (Luhn algorithm)
   - Token nên được mã hóa từ phía client trước khi gửi lên server

3. **Payment Method Types**:
   - **CREDIT_CARD/DEBIT_CARD**: Cần provider (Visa, MasterCard, JCB...) và token (số thẻ)
   - **E_WALLET**: Cần provider (Momo, ZaloPay, VNPay) và token (số điện thoại hoặc ID ví)

4. **Delete Restrictions**:
   - Không xóa được phương thức đang dùng cho subscription
   - Không xóa được phương thức có payment đang pending

5. **Integration với Payment Gateway**:
   - Trong production, cần tích hợp với payment gateway thực (Stripe, VNPay, etc.)
   - API này chỉ lưu thông tin, không xử lý thanh toán thực tế
   - Payment processing được thực hiện ở service layer
