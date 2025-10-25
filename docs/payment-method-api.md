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
- `BANK_TRANSFER`: Chuyển khoản ngân hàng
- `CASH`: Tiền mặt

---

## API Endpoints

### 1. Thêm phương thức thanh toán

**Endpoint**: `POST /api/payment-methods`

**Mô tả**: Driver thêm phương thức thanh toán mới vào tài khoản.

**Quyền truy cập**: DRIVER

**Request Body**:
```json
{
  "type": "CREDIT_CARD",
  "cardNumber": "4111111111111111",
  "cardHolderName": "NGUYEN VAN A",
  "expiryDate": "12/26",
  "isDefault": true
}
```

**Request Fields**:
- `type` (string, required): Loại thanh toán (CREDIT_CARD | DEBIT_CARD | E_WALLET | BANK_TRANSFER | CASH)
- `cardNumber` (string, optional): Số thẻ (cho CREDIT_CARD, DEBIT_CARD)
- `cardHolderName` (string, optional): Tên chủ thẻ
- `expiryDate` (string, optional): Ngày hết hạn (MM/YY)
- `eWalletPhone` (string, optional): Số điện thoại ví điện tử (cho E_WALLET)
- `eWalletProvider` (string, optional): Nhà cung cấp ví (Momo, ZaloPay, VNPay)
- `bankName` (string, optional): Tên ngân hàng (cho BANK_TRANSFER)
- `accountNumber` (string, optional): Số tài khoản
- `isDefault` (boolean, optional): Đặt làm phương thức mặc định (mặc định: false)

**Response Success** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "pm-uuid-1",
    "type": "CREDIT_CARD",
    "cardNumber": "************1111",
    "cardHolderName": "NGUYEN VAN A",
    "expiryDate": "12/26",
    "isDefault": true,
    "createdAt": "2025-10-25T10:30:00"
  }
}
```

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
      "id": "pm-uuid-1",
      "type": "CREDIT_CARD",
      "cardNumber": "************1111",
      "cardHolderName": "NGUYEN VAN A",
      "expiryDate": "12/26",
      "isDefault": true,
      "createdAt": "2025-10-25T10:30:00"
    },
    {
      "id": "pm-uuid-2",
      "type": "E_WALLET",
      "eWalletProvider": "Momo",
      "eWalletPhone": "0901234567",
      "isDefault": false,
      "createdAt": "2025-10-20T15:45:00"
    },
    {
      "id": "pm-uuid-3",
      "type": "BANK_TRANSFER",
      "bankName": "Vietcombank",
      "accountNumber": "1234567890",
      "isDefault": false,
      "createdAt": "2025-10-18T09:00:00"
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
   - Ngày hết hạn phải trong tương lai
   - Số điện thoại phải đúng format Việt Nam

3. **Default Payment Method**:
   - Nếu đặt một phương thức làm default, các phương thức khác tự động bỏ flag default
   - Phương thức default được sử dụng tự động khi thanh toán subscription

4. **Payment Method Types**:
   - **CREDIT_CARD/DEBIT_CARD**: Cần cardNumber, cardHolderName, expiryDate
   - **E_WALLET**: Cần eWalletProvider, eWalletPhone
   - **BANK_TRANSFER**: Cần bankName, accountNumber
   - **CASH**: Không cần thông tin thêm (thanh toán trực tiếp tại trạm)

5. **Delete Restrictions**:
   - Không xóa được phương thức đang dùng cho subscription
   - Không xóa được phương thức có payment đang pending
   - Nên vô hiệu hóa thay vì xóa để giữ lịch sử

6. **Integration với Payment Gateway**:
   - Trong production, cần tích hợp với payment gateway thực (Stripe, VNPay, etc.)
   - API này chỉ lưu thông tin, không xử lý thanh toán thực tế
   - Payment processing được thực hiện ở service layer
