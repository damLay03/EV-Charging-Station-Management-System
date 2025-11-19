# Hướng Dẫn Setup Cloudinary cho Vehicle Images

## 1. Tạo Tài Khoản Cloudinary (Free)

1. Truy cập: https://cloudinary.com/users/register/free
2. Đăng ký tài khoản miễn phí
3. Sau khi đăng ký, vào Dashboard để lấy **Cloud Name** của bạn

## 2. Upload Hình Ảnh Xe Lên Cloudinary

### Upload trực tiếp trên Cloudinary Dashboard:

1. Đăng nhập vào Cloudinary Dashboard
2. Vào **Media Library** → **Upload**
3. Tạo folder tên: `ev-charging-vehicles`
4. Upload 18 ảnh xe vào folder này với tên file đúng format (xem danh sách bên dưới)

### Sử dụng Cloudinary Transformation (Modify ảnh):

Khi upload, bạn có thể:
- Resize/crop ảnh
- Thêm filter/effects
- Optimize chất lượng
- Tất cả thao tác này làm trực tiếp trên Cloudinary Dashboard

## 3. Danh Sách Ảnh Cần Upload

Bạn cần upload 18 ảnh xe với tên file như sau:

### VinFast Models (6 ảnh):
- `vinfast-vf5.jpg`
- `vinfast-vf6.jpg`
- `vinfast-vf7.jpg`
- `vinfast-vf8.jpg`
- `vinfast-vf9.jpg`
- `vinfast-vfe34.jpg`

### Tesla Models (6 ảnh):
- `tesla-model-3.jpg`
- `tesla-model-3-long-range.jpg`
- `tesla-model-y.jpg`
- `tesla-model-y-long-range.jpg`
- `tesla-model-s.jpg`
- `tesla-model-x.jpg`

### BYD Models (6 ảnh):
- `byd-atto-3.jpg`
- `byd-dolphin.jpg`
- `byd-seal.jpg`
- `byd-han.jpg`
- `byd-tang.jpg`
- `byd-yuan-plus.jpg`

## 4. Lấy URL Cloudinary và Cập Nhật Code

Sau khi upload xong, mỗi ảnh sẽ có URL dạng:
```
https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567890/ev-charging-vehicles/vinfast-vf5.jpg
```

### Cập nhật VehicleModel.java:

1. Mở file: `src/main/java/com/swp/evchargingstation/enums/VehicleModel.java`
2. Thay **YOUR_CLOUD_NAME** bằng Cloud Name thực tế của bạn
3. Copy URL đầy đủ của từng ảnh từ Cloudinary và paste vào enum

**Ví dụ:**
```java
VINFAST_VF5("VF5", VehicleBrand.VINFAST, 37.23f, "LFP", "100 kW", 100f, 
    "https://res.cloudinary.com/demo123/image/upload/v1700000000/ev-charging-vehicles/vinfast-vf5.jpg"),
```

## 5. Modify Ảnh Trên Cloudinary Dashboard

Trên Cloudinary Dashboard, bạn có thể:
- **Resize/Crop**: Chỉnh kích thước ảnh
- **Apply Effects**: Thêm filter, brightness, contrast, etc.
- **Optimize**: Chuyển đổi format (WebP, AVIF), nén dung lượng
- **Add Text/Watermark**: Thêm text hoặc logo lên ảnh

Sau khi modify, copy URL mới và cập nhật vào enum.

## 6. Frontend Lấy Ảnh Như Thế Nào?

Khi frontend gọi API để lấy thông tin vehicle, response sẽ có field `imageUrl`:

```json
{
  "model": "VINFAST_VF8",
  "modelName": "VF8",
  "brand": "VINFAST",
  "batteryCapacityKwh": 87.7,
  "imageUrl": "https://res.cloudinary.com/demo123/image/upload/.../vinfast-vf8.jpg"
}
```

Frontend chỉ cần dùng URL này trong thẻ `<img>`:
```html
<img src="{{ vehicle.imageUrl }}" alt="{{ vehicle.modelName }}" />
```

## 7. Lợi Ích Của Cách Này

✅ **Đơn giản**: Không cần viết code upload phức tạp
✅ **Không cần mạng local**: Ảnh trên cloud, access từ mọi nơi
✅ **Modify trên Dashboard**: Dễ dàng chỉnh sửa ảnh trực quan
✅ **CDN toàn cầu**: Tốc độ load nhanh
✅ **Free tier**: 25GB storage + 25GB bandwidth/tháng

## 8. Best Practices

1. **Đặt tên file đúng**: Sử dụng đúng tên như trong danh sách (vd: `vinfast-vf5.jpg`)
2. **Upload ảnh chất lượng cao**: Ảnh sẽ hiển thị đẹp hơn trên frontend
3. **Modify trên Cloudinary**: Dùng Dashboard để resize/optimize thay vì xử lý local
4. **Copy URL chính xác**: Nhớ copy full URL từ Cloudinary và paste vào enum
5. **Backup ảnh gốc**: Giữ bản backup ảnh gốc ở máy tính

## 9. Troubleshooting

### Ảnh không hiển thị trên frontend:
- Kiểm tra URL trong `VehicleModel.java` có đúng không
- Thử mở URL trực tiếp trên browser xem có load được không
- Đảm bảo đã thay `YOUR_CLOUD_NAME` bằng cloud name thực tế

### URL bị 404:
- Kiểm tra folder name có đúng là `ev-charging-vehicles` không
- Kiểm tra tên file có khớp với URL không
- Đảm bảo ảnh đã được upload thành công trên Cloudinary Dashboard

