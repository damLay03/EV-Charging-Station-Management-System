# Hướng dẫn Frontend tích hợp VNPay Payment

## 🎯 Mục đích
Frontend cần tích hợp VNPay để cho phép Driver thanh toán các phiên sạc đã hoàn thành.

---

## 📋 Flow hoạt động

```
1. Driver xem lịch sử charging sessions
2. Click "Thanh toán" trên session đã hoàn thành
3. Frontend gọi API tạo payment URL
4. Frontend redirect Driver đến VNPay
5. Driver nhập thông tin thẻ và xác nhận
6. VNPay redirect về trang kết quả
7. Frontend hiển thị kết quả thanh toán
```

---

## 🔧 Các bước implement

### Bước 1: Tạo trang Charging History với nút Thanh toán

```jsx
// ChargingHistory.jsx
import React, { useState } from 'react';
import axios from 'axios';

const ChargingHistory = () => {
  const [sessions, setSessions] = useState([]);
  
  const handlePayment = async (sessionId) => {
    try {
      const response = await axios.post(
        'http://localhost:8084/evchargingstation/api/payment/vnpay/create',
        {
          sessionId: sessionId,
          bankCode: '' // Để trống để VNPay hiển thị tất cả phương thức
        },
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
      
      if (response.data.code === 1000) {
        // Redirect đến VNPay
        window.location.href = response.data.result.paymentUrl;
      }
    } catch (error) {
      console.error('Payment error:', error);
      alert('Có lỗi xảy ra khi tạo thanh toán!');
    }
  };
  
  return (
    <div>
      <h2>Lịch sử sạc xe</h2>
      {sessions.map(session => (
        <div key={session.sessionId} className="session-card">
          <p>Trạm: {session.stationName}</p>
          <p>Năng lượng: {session.energyKwh} kWh</p>
          <p>Chi phí: {session.costTotal.toLocaleString()} VNĐ</p>
          <p>Trạng thái: {session.status}</p>
          
          {session.status === 'COMPLETED' && !session.isPaid && (
            <button onClick={() => handlePayment(session.sessionId)}>
              Thanh toán
            </button>
          )}
          
          {session.isPaid && (
            <span className="paid-badge">✓ Đã thanh toán</span>
          )}
        </div>
      ))}
    </div>
  );
};
```

### Bước 2: Tạo trang Payment Result

Tạo route `/payment-result` trong React Router:

```jsx
// PaymentResult.jsx
import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const PaymentResult = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [paymentInfo, setPaymentInfo] = useState(null);
  
  useEffect(() => {
    // Lấy query parameters
    const params = new URLSearchParams(location.search);
    const responseCode = params.get('vnp_ResponseCode');
    const sessionId = params.get('vnp_TxnRef');
    const amount = parseInt(params.get('vnp_Amount')) / 100;
    const transactionId = params.get('vnp_TransactionNo');
    const bankCode = params.get('vnp_BankCode');
    
    setPaymentInfo({
      success: responseCode === '00',
      sessionId,
      amount,
      transactionId,
      bankCode,
      responseCode
    });
    
    // Tự động redirect sau 5s
    if (responseCode === '00') {
      setTimeout(() => {
        navigate('/charging-history');
      }, 5000);
    }
  }, [location, navigate]);
  
  if (!paymentInfo) return <div>Đang xử lý...</div>;
  
  return (
    <div className="payment-result">
      {paymentInfo.success ? (
        <div className="success">
          <h1>✓ Thanh toán thành công!</h1>
          <p>Số tiền: {paymentInfo.amount.toLocaleString()} VNĐ</p>
          <p>Mã giao dịch: {paymentInfo.transactionId}</p>
          <p>Phiên sạc: {paymentInfo.sessionId}</p>
          <p>Ngân hàng: {paymentInfo.bankCode}</p>
          <p className="redirect-info">
            Bạn sẽ được chuyển về trang lịch sử sau 5 giây...
          </p>
        </div>
      ) : (
        <div className="error">
          <h1>✗ Thanh toán thất bại</h1>
          <p>Mã lỗi: {paymentInfo.responseCode}</p>
          <p>Vui lòng thử lại hoặc chọn phương thức thanh toán khác.</p>
          <button onClick={() => navigate('/charging-history')}>
            Quay lại lịch sử
          </button>
        </div>
      )}
    </div>
  );
};

export default PaymentResult;
```

### Bước 3: Cấu hình React Router

```jsx
// App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ChargingHistory from './pages/ChargingHistory';
import PaymentResult from './pages/PaymentResult';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/charging-history" element={<ChargingHistory />} />
        <Route path="/payment-result" element={<PaymentResult />} />
        {/* Other routes */}
      </Routes>
    </BrowserRouter>
  );
}
```

---

## 🎨 UI/UX Recommendations

### 1. Button "Thanh toán"
```css
.payment-button {
  background: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.payment-button:hover {
  background: #45a049;
}
```

### 2. Payment Result Page
```css
.payment-result {
  max-width: 500px;
  margin: 50px auto;
  padding: 30px;
  text-align: center;
}

.success {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
  padding: 20px;
  border-radius: 10px;
}

.error {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
  padding: 20px;
  border-radius: 10px;
}
```

---

## 🔐 Thông tin test VNPay

### Ngân hàng NCB (khuyến nghị)
- **Số thẻ**: `9704198526191432198`
- **Tên chủ thẻ**: `NGUYEN VAN A`
- **Ngày phát hành**: `07/15`
- **Mật khẩu OTP**: `123456`

### Thẻ quốc tế
- **Số thẻ**: `4111111111111111`
- **Tên chủ thẻ**: `NGUYEN VAN A`
- **Ngày hết hạn**: `12/25`
- **CVV**: `123`

---

## 📝 Response Codes từ VNPay

| Code | Meaning |
|------|---------|
| 00 | Giao dịch thành công |
| 07 | Trừ tiền thành công, giao dịch nghi vấn |
| 09 | Thẻ chưa đăng ký dịch vụ |
| 10 | Thẻ hết hạn hoặc sai thông tin |
| 11 | Thẻ hết hạn |
| 12 | Thẻ bị khóa |
| 24 | Giao dịch bị hủy |
| 51 | Tài khoản không đủ số dư |
| 65 | Nhập sai OTP quá số lần quy định |

---

## 🚀 Testing Checklist

- [ ] Test với sessionId thực tế từ database
- [ ] Test thanh toán thành công (OTP: 123456)
- [ ] Test thanh toán thất bại (nhập sai OTP)
- [ ] Test với các loại thẻ khác nhau
- [ ] Verify payment status được cập nhật trong database
- [ ] Test redirect về payment-result page
- [ ] Test UI hiển thị kết quả đúng

---

## ⚠️ Lưu ý Production

1. **Return URL**: Khi deploy, cập nhật `vnpay.returnUrl` trong `application.yaml`:
   ```yaml
   vnpay:
     returnUrl: https://yourdomain.com/payment-result
   ```

2. **CORS**: Đảm bảo backend cho phép frontend domain trong CORS config

3. **HTTPS**: VNPay yêu cầu HTTPS cho production

4. **Error Handling**: Xử lý tất cả trường hợp lỗi có thể xảy ra

5. **Loading State**: Hiển thị loading khi đang tạo payment URL

---

## 📞 Support

Nếu có vấn đề:
1. Check browser console logs
2. Check backend logs
3. Verify payment status trong database
4. Contact VNPay support nếu có lỗi từ phía VNPay

