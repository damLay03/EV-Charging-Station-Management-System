# Danh sách các lỗi đã sửa trong API Documentation

## Tổng quan
Frontend báo endpoints đúng nhưng fields trong response/request bị sai rất nhiều. Đây là danh sách các vấn đề đã được sửa:

---

## 1. authentication-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Response của `/api/auth/login` thiếu field `userInfo`

### Đã sửa:
- Thêm object `userInfo` vào response chứa thông tin chi tiết user:
  - `userId`, `email`, `phone`, `dateOfBirth`, `gender`
  - `firstName`, `lastName`, `fullName`, `role`

---

## 2. user-management-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Request body của `/api/users/register` sai hoàn toàn
- Response của các endpoints thiếu/dư nhiều fields

### Đã sửa:

#### Register endpoint:
- **Request**: Chỉ cần `email`, `password`, `confirmPassword` (loại bỏ fullName, phoneNumber, address)
- **Response**: Trả về UserResponse với các fields: `userId`, `email`, `phone`, `dateOfBirth`, `gender`, `firstName`, `lastName`, `fullName`, `role`

#### GET /api/users/driver/myInfo:
- **Response**: Trả về DriverResponse (có thêm `address` và `joinDate`)

#### PATCH /api/users/driver/myInfo:
- **Request**: Chỉ các fields: `phone`, `dateOfBirth`, `gender`, `firstName`, `lastName`, `address` (không có password, fullName)
- **Response**: Trả về DriverResponse

#### GET /api/users (Admin):
- **Response**: Trả về AdminUserResponse với fields:
  - `fullName`, `email`, `phone`, `joinDate`, `planName`
  - `sessionCount`, `totalSpent`, `status`, `isActive`

---

## 3. station-management-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Thiếu fields `latitude`, `longitude` trong response
- Thiếu fields `staffId`, `staffName`
- Request body thiếu fields quan trọng

### Đã sửa:

#### POST /api/stations/create:
- **Request**: Thêm `numberOfChargingPoints`, `powerOutput`, `latitude`, `longitude`, `staffId`

#### GET /api/stations, /api/stations/overview, /api/stations/detail:
- **Response StationResponse**: Thêm `latitude`, `longitude`, `staffId`, `staffName`, `active`
- **Response StationDetailResponse**: Thêm chi tiết về charging points:
  - `totalChargingPoints`, `activeChargingPoints`, `offlineChargingPoints`, `maintenanceChargingPoints`
  - `chargingPointsSummary`, `revenue`, `usagePercent`

---

## 4. payment-method-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Request/Response có quá nhiều fields phức tạp không tồn tại trong DTO
- Cấu trúc dữ liệu khác hoàn toàn với implementation

### Đã sửa:

#### POST /api/payment-methods:
- **Request**: Chỉ 3 fields: `methodType`, `provider`, `token`
- **Response**: `pmId`, `methodType`, `provider`, `maskedToken`

#### Loại bỏ các fields không tồn tại:
- `cardNumber`, `cardHolderName`, `expiryDate`
- `eWalletPhone`, `eWalletProvider`
- `bankName`, `accountNumber`, `isDefault`, `createdAt`

---

## 5. dashboard-driver-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Response của `/api/dashboard/summary` có fields không đúng

### Đã sửa:

#### GET /api/dashboard/summary:
- **Response DashboardSummaryResponse**: 
  - `totalRevenue` (thay vì totalCost)
  - `totalEnergyUsed` (thay vì totalEnergyKwh)
  - `totalSessions`
  - `averagePricePerKwh` (thay vì averageCostPerSession, averageDuration, periodStart, periodEnd)

---

## 6. notification-settings-api.md ✅ ĐÃ SỬA

### Vấn đề:
- Cấu trúc dữ liệu hoàn toàn sai - documentation dùng boolean fields (emailEnabled, smsEnabled...) nhưng implementation dùng (notificationType + channel)

### Đã sửa:

#### GET /api/notification:
- **Response**: Mỗi setting là một object với:
  - `settingId`
  - `notificationType`
  - `channel` (EMAIL | SMS | PUSH | IN_APP)
  - `isEnabled`

#### PUT /api/notification (Batch update):
- **Request**: Array of settings, mỗi setting có `notificationType`, `channel`, `isEnabled`

---

## Các file còn cần kiểm tra:

- [ ] **vehicles-api.md** - Đã đúng (refactored gần đây)
- [ ] **charging-simulation-api.md** - Đã đúng
- [ ] **plan-api.md** - Cần kiểm tra response fields (float vs number)
- [ ] **admin-dashboard-api.md** - Cần kiểm tra chi tiết
- [ ] **staff-dashboard-api.md** - File trống, cần tạo nội dung

---

## Lưu ý cho Frontend:

1. **Kiểu dữ liệu**: 
   - Java `float` → JSON `number`
   - Java `LocalDate` → JSON `string` (yyyy-MM-dd)
   - Java `LocalDateTime` → JSON `string` (ISO 8601)

2. **Nullable fields**: Nhiều fields có thể null, cần handle null checking

3. **Enum values**: Phải khớp chính xác với enum trong backend (case-sensitive)

4. **ID fields**: 
   - `userId` (không phải `id`)
   - `stationId` (không phải `id`)
   - `vehicleId` (không phải `id`)
   - `pmId` cho payment method (không phải `id`)

5. **Computed fields**: Một số fields được tính toán (getters) trong VehicleResponse:
   - `brand`, `brandDisplayName`, `modelName`, `batteryCapacityKwh`, `batteryType`

