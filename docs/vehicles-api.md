# Vehicles API

Tài liệu này mô tả đầy đủ API quản lý xe: các endpoint Public (brands/models), Driver (CRUD xe của tôi) và Admin (liệt kê xe theo driver).

- Base URL: `http://localhost:8080`
- Bảo mật:
  - Driver endpoints yêu cầu Bearer JWT có vai trò `DRIVER`.
  - Admin endpoint yêu cầu Bearer JWT có vai trò `ADMIN`.
  - Public endpoints (brands/models) theo thiết kế là public. Nếu môi trường của bạn yêu cầu xác thực cho mọi route, hãy thêm permitAll cho các đường dẫn này trong `SecurityConfig`.

---

## 1) Enums & Model cố định

### VehicleBrand
- `VINFAST` — Việt Nam
- `TESLA` — Mỹ
- `BYD` — Trung Quốc

### VehicleModel (Cố định theo Brand, kèm dung lượng pin và loại pin)
- VINFAST: `VINFAST_VF5`, `VINFAST_VF6`, `VINFAST_VF7`, `VINFAST_VF8`, `VINFAST_VF9`, `VINFAST_VFE34`
- TESLA: `TESLA_MODEL_3`, `TESLA_MODEL_3_LONG_RANGE`, `TESLA_MODEL_Y`, `TESLA_MODEL_Y_LONG_RANGE`, `TESLA_MODEL_S`, `TESLA_MODEL_X`
- BYD: `BYD_ATTO_3`, `BYD_DOLPHIN`, `BYD_SEAL`, `BYD_HAN`, `BYD_TANG`, `BYD_YUAN_PLUS`

Ghi chú:
- Mỗi model có các thuộc tính: `modelName` (ví dụ "VF8", "Model 3 Long Range"), `brand`, `batteryCapacityKwh`, `batteryType`.
- Khi tạo/cập nhật xe, `batteryCapacityKwh` và `batteryType` sẽ tự động lấy theo `model` đã chọn.

---

## 2) DTO

### 2.1) VehicleCreationRequest (request body khi tạo xe)
```json
{
  "licensePlate": "30A-12345",
  "brand": "VINFAST",
  "model": "VINFAST_VF8"
}
```
- `licensePlate`: string, bắt buộc, duy nhất
- `brand`: enum VehicleBrand, bắt buộc
- `model`: enum VehicleModel, bắt buộc (phải thuộc đúng brand)

### 2.2) VehicleUpdateRequest (request body khi cập nhật xe; mọi field đều tùy chọn)
```json
{
  "licensePlate": "30A-99999",
  "brand": "TESLA",
  "model": "TESLA_MODEL_3"
}
```
- Có thể chỉ cập nhật một phần. Nếu cập nhật `brand` hoặc `model`, hệ thống sẽ kiểm tra `model` có thuộc `brand` không.
- Nếu đổi `model`, hệ thống tự cập nhật `batteryCapacityKwh` và `batteryType` theo model mới.

### 2.3) VehicleResponse (response chung cho create/get/update)
```json
{
  "vehicleId": "...",
  "licensePlate": "30A-12345",
  "brand": "VINFAST",
  "brandDisplayName": "VinFast",
  "model": "VINFAST_VF8",
  "modelName": "VF8",
  "batteryCapacityKwh": 87.7,
  "batteryType": "NMC",
  "ownerId": "..."
}
```

### 2.4) VehicleBrandResponse (public)
```json
{
  "brand": "VINFAST",
  "displayName": "VinFast",
  "country": "Việt Nam"
}
```

### 2.5) VehicleModelResponse (public)
```json
{
  "model": "VINFAST_VF8",
  "modelName": "VF8",
  "brand": "VINFAST",
  "batteryCapacityKwh": 87.7,
  "batteryType": "NMC"
}
```

---

## 3) Endpoints

### 3.1) Public (brands/models)
- `GET /api/vehicles/brands`
  - Trả về danh sách `VehicleBrandResponse[]`.

- `GET /api/vehicles/brands/{brand}/models`
  - Path var `brand` ∈ {`BYD`,`VINFAST`,`TESLA`}
  - Trả về danh sách `VehicleModelResponse[]` theo brand.

- `GET /api/vehicles/models`
  - Trả về toàn bộ `VehicleModelResponse[]`.

### 3.2) Driver (quản lý xe của tôi)
- `POST /api/vehicles` — Tạo xe mới (Bearer DRIVER)
  - Body: `VehicleCreationRequest`
  - Validate:
    - Biển số không trùng (`LICENSE_PLATE_EXISTED`)
    - `model` thuộc `brand` (`INVALID_VEHICLE_MODEL_FOR_BRAND`)
  - Kết quả: `VehicleResponse` (kèm thông tin pin tự điền từ model)

- `GET /api/vehicles/my-vehicles` — Danh sách xe của tôi (Bearer DRIVER)
  - Kết quả: `VehicleResponse[]`

- `GET /api/vehicles/my-vehicles/{vehicleId}` — Chi tiết xe (Bearer DRIVER)
  - Chỉ xem được xe thuộc sở hữu (`VEHICLE_NOT_BELONG_TO_DRIVER` nếu vi phạm)
  - Kết quả: `VehicleResponse`

- `PUT /api/vehicles/{vehicleId}` — Cập nhật xe (Bearer DRIVER)
  - Body: `VehicleUpdateRequest` (tùy chọn)
  - Validate: tương tự khi tạo; riêng thay biển số sẽ kiểm tra không trùng với xe khác
  - Nếu đổi `model`, thông tin pin tự cập nhật
  - Kết quả: `VehicleResponse`

- `DELETE /api/vehicles/{vehicleId}` — Xóa xe (Bearer DRIVER)
  - Chỉ xóa được xe thuộc sở hữu
  - Kết quả: `ApiResponse<Void>` với message "Vehicle deleted successfully"

### 3.3) Admin
- `GET /api/vehicles/driver/{driverId}` — Danh sách xe theo driver (Bearer ADMIN)
  - Kết quả: `VehicleResponse[]`

---

## 4) Quy tắc & Validate
- Ràng buộc brand-model: `model.getBrand() == brand`. Vi phạm trả `INVALID_VEHICLE_MODEL_FOR_BRAND`.
- Biển số (`licensePlate`) là duy nhất. Trùng trả `LICENSE_PLATE_EXISTED`.
- Khi chọn `model`, hệ thống tự điền `batteryCapacityKwh` và `batteryType` từ enum `VehicleModel`.
- Người dùng chỉ được phép thao tác trên xe của chính họ. Vi phạm trả `VEHICLE_NOT_BELONG_TO_DRIVER`/`UNAUTHORIZED`.

---

## 5) Mã lỗi tiêu biểu (ErrorCode)
- `VEHICLE_NOT_FOUND`
- `LICENSE_PLATE_EXISTED`
- `VEHICLE_NOT_BELONG_TO_DRIVER`
- `INVALID_VEHICLE_MODEL_FOR_BRAND`
- `USER_NOT_FOUND` / `UNAUTHORIZED`

---

## 6) Ví dụ cURL (Windows cmd)
Thiết lập biến chung:
```cmd
set BASE_URL=http://localhost:8080
set TOKEN=<JWT_DRIVER>
set ADMIN_TOKEN=<JWT_ADMIN>
```

### 6.1) Public
- Lấy brands:
```cmd
curl -X GET "%BASE_URL%/api/vehicles/brands" -H "Accept: application/json"
```
- Lấy models theo brand (ví dụ VINFAST):
```cmd
curl -X GET "%BASE_URL%/api/vehicles/brands/VINFAST/models" -H "Accept: application/json"
```
- Lấy tất cả models:
```cmd
curl -X GET "%BASE_URL%/api/vehicles/models" -H "Accept: application/json"
```

### 6.2) Driver
- Tạo xe mới:
```cmd
curl -X POST "%BASE_URL%/api/vehicles" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"licensePlate\":\"30A-12345\",\"brand\":\"VINFAST\",\"model\":\"VINFAST_VF8\"}"
```
- Lấy danh sách xe của tôi:
```cmd
curl -X GET "%BASE_URL%/api/vehicles/my-vehicles" -H "Authorization: Bearer %TOKEN%"
```
- Lấy chi tiết 1 xe của tôi:
```cmd
set VEHICLE_ID=<ID_VEHICLE>
curl -X GET "%BASE_URL%/api/vehicles/my-vehicles/%VEHICLE_ID%" -H "Authorization: Bearer %TOKEN%"
```
- Cập nhật xe (đổi model):
```cmd
curl -X PUT "%BASE_URL%/api/vehicles/%VEHICLE_ID%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"model\":\"VINFAST_VF6\"}"
```
- Xóa xe:
```cmd
curl -X DELETE "%BASE_URL%/api/vehicles/%VEHICLE_ID%" -H "Authorization: Bearer %TOKEN%"
```

### 6.3) Admin
- Lấy xe theo driverId:
```cmd
set DRIVER_ID=<ID_DRIVER>
curl -X GET "%BASE_URL%/api/vehicles/driver/%DRIVER_ID%" -H "Authorization: Bearer %ADMIN_TOKEN%"
```

---

## 7) Gợi ý FE
- Với endpoints public (brands/models), FE có thể điều chỉnh form: khi chọn `brand`, gọi `GET /brands/{brand}/models` để fill danh sách `model` hợp lệ.
- Với danh sách/chi tiết xe, hiển thị `modelName` và `brandDisplayName` để UX thân thiện.
- Với trường hợp rỗng (chưa có xe), API trả `[]` thay vì lỗi; FE hiển thị trạng thái "chưa có xe".

---

## 8) Ghi chú bảo mật
- Nếu muốn cho phép public endpoints không cần token, đảm bảo thêm các đường dẫn sau vào mảng `PUBLIC_ENDPOINTS` trong `SecurityConfig`:
  - `/api/vehicles/brands`, `/api/vehicles/brands/*/models`, `/api/vehicles/models`
- Ngược lại, nếu bắt buộc xác thực cho tất cả, FE cần gắn token cả với endpoints public.

