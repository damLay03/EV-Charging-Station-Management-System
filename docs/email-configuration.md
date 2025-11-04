# Hướng dẫn cấu hình Email tự động

## Tổng quan

Hệ thống tự động gửi email cho người dùng khi:
1. **Bắt đầu sạc**: Thông báo phiên sạc đã được khởi tạo
2. **Kết thúc sạc**: Tóm tắt phiên sạc và hóa đơn
3. **Xác nhận thanh toán**: Xác nhận thanh toán thành công

## Cấu hình Gmail SMTP

### Bước 1: Tạo App Password cho Gmail

1. Đăng nhập vào tài khoản Gmail của bạn
2. Truy cập: https://myaccount.google.com/security
3. Bật **2-Step Verification** (nếu chưa bật)
4. Tìm **App passwords** hoặc truy cập: https://myaccount.google.com/apppasswords
5. Chọn **Select app** → **Other (Custom name)**
6. Nhập tên: `EV Charging System`
7. Click **Generate**
8. Copy **16-digit password** (ví dụ: `abcd efgh ijkl mnop`)

### Bước 2: Cấu hình application.yaml

Mở file `src/main/resources/application.yaml` và cập nhật:

```yaml
mail:
  host: smtp.gmail.com
  port: 587
  username: your-email@gmail.com         # Thay bằng email của bạn
  password: abcd efgh ijkl mnop          # Thay bằng App Password (16 ký tự)
  properties:
    mail:
      smtp:
        auth: true
        starttls:
          enable: true
          required: true
        connectiontimeout: 5000
        timeout: 5000
        writetimeout: 5000
  from: EV Charging System <your-email@gmail.com>  # Thay bằng email của bạn
```

### Bước 3: Sử dụng Environment Variables (Khuyến nghị)

**Cách 1: Sử dụng IntelliJ IDEA**

1. **Run** → **Edit Configurations**
2. Chọn configuration của application
3. Trong **Environment variables**, thêm:
   ```
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=abcdefghijklmnop
   MAIL_FROM=EV Charging System <your-email@gmail.com>
   ```

**Cách 2: Command Line**

Windows:
```cmd
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=abcdefghijklmnop
set MAIL_FROM=EV Charging System <your-email@gmail.com>
.\mvnw.cmd spring-boot:run
```

Linux/Mac:
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=abcdefghijklmnop
export MAIL_FROM="EV Charging System <your-email@gmail.com>"
./mvnw spring-boot:run
```

## Kiểm tra cấu hình

### Test gửi email khi bắt đầu sạc

1. Đăng nhập với tài khoản driver
2. Chọn xe và trạm sạc
3. Bắt đầu phiên sạc
4. Kiểm tra email (cả inbox và spam folder)

### Log kiểm tra

Kiểm tra console log:
```
INFO  c.s.e.service.EmailService - Sent charging start email to user@example.com for session abc123
```

Nếu có lỗi:
```
ERROR c.s.e.service.EmailService - Failed to send charging start email: AuthenticationFailedException
```
→ Kiểm tra lại username/password

## Các template email

### 1. Email bắt đầu sạc
- **Subject**: ⚡ Phiên sạc của bạn đã bắt đầu
- **Nội dung**: Thông tin trạm, thời gian, mức pin hiện tại và mục tiêu

### 2. Email kết thúc sạc
- **Subject**: ✅ Phiên sạc của bạn đã hoàn tất
- **Nội dung**: Tóm tắt thời gian sạc, năng lượng, chi phí

### 3. Email xác nhận thanh toán
- **Subject**: 💳 Thanh toán thành công
- **Nội dung**: Số tiền, thời gian thanh toán, mã giao dịch

## Troubleshooting

### Lỗi: AuthenticationFailedException

**Nguyên nhân**: Sai username hoặc password

**Giải pháp**:
1. Kiểm tra lại email có đúng không
2. Tạo lại App Password mới
3. Đảm bảo không có khoảng trắng trong password
4. Kiểm tra 2-Step Verification đã bật

### Lỗi: Connection timeout

**Nguyên nhân**: Firewall hoặc network chặn port 587

**Giải pháp**:
1. Kiểm tra firewall cho phép outbound port 587
2. Thử đổi port sang 465 (SSL):
   ```yaml
   mail:
     port: 465
     properties:
       mail:
         smtp:
           ssl:
             enable: true
   ```

### Lỗi: Email vào Spam

**Giải pháp**:
1. Thêm địa chỉ email hệ thống vào contact
2. Đánh dấu "Not spam"
3. Tạo filter để email luôn vào Inbox

### Email không nhận được

**Kiểm tra**:
1. Xem log console có thông báo "Sent email" không
2. Kiểm tra spam folder
3. Kiểm tra email address trong database có đúng không
4. Kiểm tra quota Gmail (mỗi ngày giới hạn ~500 email)

## Tính năng nâng cao

### Tắt gửi email khi develop

Thêm vào `application.yaml`:

```yaml
spring:
  mail:
    enabled: false  # Tắt gửi email
```

### Sử dụng SMTP khác (không phải Gmail)

**SendGrid**:
```yaml
mail:
  host: smtp.sendgrid.net
  port: 587
  username: apikey
  password: YOUR_SENDGRID_API_KEY
```

**Mailgun**:
```yaml
mail:
  host: smtp.mailgun.org
  port: 587
  username: postmaster@yourdomain.mailgun.org
  password: YOUR_MAILGUN_PASSWORD
```

## Bảo mật

⚠️ **QUAN TRỌNG**:
- **KHÔNG** commit App Password vào Git
- Luôn sử dụng Environment Variables cho production
- Thêm `application-local.yaml` vào `.gitignore`
- Rotate App Password định kỳ

## Giới hạn Gmail

- **500 emails/ngày** cho tài khoản Gmail thường
- **2000 emails/ngày** cho Google Workspace
- Nếu vượt quota, cân nhắc dùng dịch vụ email chuyên nghiệp (SendGrid, AWS SES, etc.)

## Tham khảo

- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [JavaMail API](https://javaee.github.io/javamail/)

