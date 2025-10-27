# VNPay Integration - Tuân Thủ Tài Liệu Chính Thức

## Tài liệu tham khảo
https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html

## Vấn đề đã được sửa
VNPay yêu cầu **tuân thủ chặt chẽ** cách tạo SecureHash và URL theo tài liệu chính thức.

## URL mẫu từ VNPay
```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=1806000&vnp_Command=pay&vnp_CreateDate=20210801153333&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+%3A5&vnp_OrderType=other&vnp_ReturnUrl=https%3A%2F%2Fdomainmerchant.vn%2FReturnUrl&vnp_TmnCode=DEMOV210&vnp_TxnRef=5&vnp_Version=2.1.0&vnp_SecureHash=3e0d61a0c0534b2e36680b3f7277743e8784cc4e1d68fa7d276e79c23be7d6318d338b477910a27992f5057bb1582bd44bd82ae8009ffaf6d141219218625c42
```

## ✅ Giải pháp - Tuân Thủ Chặt Chẽ Tài Liệu VNPay

### 1. Hash Data - QUAN TRỌNG NHẤT

Theo tài liệu VNPay chính thức:
> Chuỗi dữ liệu hash được tạo từ các tham số **KHÔNG mã hóa URL**, sắp xếp theo thứ tự field name (A-Z)

**Hash Data (KHÔNG encode):**
```
vnp_Amount=10000&vnp_Command=pay&vnp_CreateDate=20210801153333&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Thanh toan don hang :5&vnp_OrderType=other&vnp_ReturnUrl=https://domainmerchant.vn/ReturnUrl&vnp_TmnCode=DEMOV210&vnp_TxnRef=5&vnp_Version=2.1.0
```

**Lưu ý**: Không có ký tự encode (%3A, +, %2F, v.v.) - giữ nguyên hoàn toàn!

### 2. Query URL

**Query URL (CÓ encode):**
```
vnp_Amount=10000&vnp_Command=pay&vnp_CreateDate=20210801153333&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+%3A5&vnp_OrderType=other&vnp_ReturnUrl=https%3A%2F%2Fdomainmerchant.vn%2FReturnUrl&vnp_TmnCode=DEMOV210&vnp_TxnRef=5&vnp_Version=2.1.0
```

**Lưu ý**: Có encode (dấu cách → +, : → %3A, / → %2F)

### 3. Implementation trong VNPayUtil

```java
public static String getPaymentURL(Map<String, String> paramsMap, boolean encodeKey) {
    // Sắp xếp theo alphabet (A-Z)
    List<String> fieldNames = new ArrayList<>(paramsMap.keySet());
    Collections.sort(fieldNames);
    
    StringBuilder sb = new StringBuilder();
    for (String fieldName : fieldNames) {
        String fieldValue = paramsMap.get(fieldName);
        if (fieldValue != null && !fieldValue.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            
            if (encodeKey) {
                // Query URL: encode cả key và value
                sb.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
            } else {
                // Hash data: KHÔNG encode gì cả
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
        }
    }
    
    return sb.toString();
}
```

## 4. Cách sử dụng trong VNPayService

```java
// 1. Lấy config với các tham số mặc định
Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();

// 2. Thêm các tham số động
vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
if (bankCode != null && !bankCode.isEmpty()) {
    vnpParamsMap.put("vnp_BankCode", bankCode);
}

// 3. Tạo query URL (CÓ encode)
String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);

// 4. Tạo hash data (KHÔNG encode)
String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);

// 5. Tính SecureHash từ hash data
String vnpSecureHash = VNPayUtil.hmacSHA512(secretKey, hashData);

// 6. Tạo payment URL
queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
String paymentUrl = vnp_PayUrl + "?" + queryUrl;
```

## 5. Các tham số bắt buộc theo VNPay

### Tham số bắt buộc:
- ✅ `vnp_Version`: "2.1.0"
- ✅ `vnp_Command`: "pay"
- ✅ `vnp_TmnCode`: Mã website (từ VNPay)
- ✅ `vnp_Amount`: Số tiền (đơn vị: VNĐ x 100)
- ✅ `vnp_CreateDate`: yyyyMMddHHmmss (GMT+7)
- ✅ `vnp_CurrCode`: "VND"
- ✅ `vnp_IpAddr`: IPv4 của khách hàng
- ✅ `vnp_Locale`: "vn" hoặc "en"
- ✅ `vnp_OrderInfo`: Mô tả đơn hàng
- ✅ `vnp_OrderType`: Loại đơn hàng
- ✅ `vnp_ReturnUrl`: URL callback
- ✅ `vnp_TxnRef`: Mã giao dịch (unique)

### Tham số tùy chọn:
- `vnp_BankCode`: Mã ngân hàng
- `vnp_ExpireDate`: Thời gian hết hạn
- `vnp_Bill_*`: Thông tin hóa đơn
- `vnp_Inv_*`: Thông tin xuất hóa đơn

## 6. Thứ tự xử lý

1. **Thu thập tham số** vào Map
2. **Sắp xếp theo alphabet** (A-Z) bằng `Collections.sort()`
3. **Tạo hash data** (KHÔNG encode) theo format: `key1=value1&key2=value2`
4. **Tính SecureHash** = HMACSHA512(secretKey, hashData)
5. **Tạo query URL** (CÓ encode) theo format: `key1=value1&key2=value2`
6. **Ghép URL cuối cùng**: `paymentUrl?queryUrl&vnp_SecureHash=hash`

## 7. So sánh Hash Data vs Query URL

### Hash Data (encodeKey = false):
```
vnp_OrderInfo=Thanh toan don hang :5
vnp_ReturnUrl=https://domainmerchant.vn/ReturnUrl
```
**Không có encode!**

### Query URL (encodeKey = true):
```
vnp_OrderInfo=Thanh+toan+don+hang+%3A5
vnp_ReturnUrl=https%3A%2F%2Fdomainmerchant.vn%2FReturnUrl
```
**Có encode!**

## 8. Debug Checklist

### ✅ Kiểm tra Hash Data:
```java
log.info("Hash data: {}", hashData);
// Phải không có ký tự encode (%3A, +, %2F)
// Ví dụ đúng: vnp_OrderInfo=Thanh toan don hang :5
// Ví dụ SAI: vnp_OrderInfo=Thanh+toan+don+hang+%3A5
```

### ✅ Kiểm tra thứ tự:
```
vnp_Amount < vnp_BankCode < vnp_Command < vnp_CreateDate < ...
```
Phải theo thứ tự alphabet!

### ✅ Kiểm tra SecureHash:
- Độ dài: 128 ký tự (hex của SHA512)
- Chữ thường (lowercase)
- Tính từ hash data KHÔNG encode

### ✅ Kiểm tra IP Address:
- Phải là IPv4 (127.0.0.1)
- KHÔNG được là IPv6 (0:0:0:0:0:0:0:1)

## 9. Test Cases

### ✅ Success Case:
```
Hash data: vnp_Amount=1000000&vnp_Command=pay&vnp_CreateDate=20251027132316&...
(không có %3A, +, %2F)

Query URL: vnp_Amount=1000000&vnp_Command=pay&vnp_CreateDate=20251027132316&vnp_OrderInfo=Thanh+toan+don+hang+%3A12345678&...
(có %3A, +, %2F)

SecureHash: a1b2c3d4e5f6...128 ký tự
```

### ❌ Failure Cases:
1. Hash data bị encode → SecureHash sai
2. Thứ tự không đúng alphabet → SecureHash sai
3. Thiếu tham số bắt buộc → VNPay từ chối
4. IP là IPv6 → VNPay từ chối

## 10. Configuration

### application.yaml:
```yaml
vnpay:
  url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  returnUrl: http://localhost:8084/evchargingstation/api/payment/vnpay-callback
  tmnCode: D18XZYI3  # Từ VNPay
  secretKey: WR0KM5KKY943UDP1SNZMFP5ZNH5ODCGU  # Từ VNPay
  version: 2.1.0
  command: pay
  orderType: other
```

## 11. Lưu ý quan trọng

### Timezone:
- Phải dùng `Etc/GMT+7` cho Việt Nam
- Format: `yyyyMMddHHmmss`

### Charset:
- Phải dùng `UTF-8` cho tất cả encode
- Hash data KHÔNG encode

### SecureHash:
- Thuật toán: HMACSHA512
- Output: lowercase hex (128 ký tự)
- Input: hash data KHÔNG encode

## 12. Tham khảo
- [VNPay Payment API](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html)
- [VNPay Sandbox](https://sandbox.vnpayment.vn/apis/)
