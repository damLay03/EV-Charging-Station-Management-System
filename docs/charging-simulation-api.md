# Charging Simulation API

Tài liệu này mô tả đầy đủ API cho tính năng giả lập sạc điện, bao gồm Start/Stop phiên sạc, xem chi tiết và lịch sử phiên sạc.

- Base URL: `http://localhost:8080`
- Bảo mật: các API yêu cầu Bearer JWT của tài khoản có vai trò `DRIVER` (trừ các API public khác không thuộc phạm vi tài liệu này). Mã nguồn đọc `userId` từ claim: `jwt.getClaim("userId")`.
- Tốc độ mô phỏng: Nhiệm vụ nền chạy mỗi 2 giây và mô phỏng 2 phút sạc thực tế cho mọi phiên có trạng thái `IN_PROGRESS`.

## 1) Mô tả hoạt động mô phỏng
- Mỗi tick (2 giây) hệ thống sẽ tính toán như sạc 2 phút thực:
  - `durationMin += 2`
  - `energyPerTick = powerKw * (2/60)`
  - `socAddedPerTick = (energyPerTick / vehicle.batteryCapacityKwh) * 100`
  - `endSocPercent` và `vehicle.currentSocPercent` tăng theo `socAddedPerTick` (làm tròn int khi lưu)
  - Khi SOC đạt/ vượt `targetSocPercent`, phiên đổi trạng thái `COMPLETED`, cập nhật `endTime`, tính chi phí, giải phóng trụ sạc và tạo `Payment` trạng thái `PENDING`.

## 2) Thay đổi dữ liệu
- Entity `ChargingSession` có thêm cột: `targetSocPercent` (Integer) để lưu mục tiêu sạc.

## 3) DTO chính
- StartChargingRequest (Body khi Start):
  - `chargingPointId`: string, bắt buộc
  - `vehicleId`: string, bắt buộc
  - `targetSocPercent`: integer, tuỳ chọn (mặc định 100 nếu không gửi)

- ChargingSessionResponse (Response khi Start/Stop/Get):
  - `sessionId`: string
  - `startTime`: string (ISO datetime)
  - `endTime`: string hoặc null
  - `durationMin`: integer
  - `stationName`: string
  - `stationAddress`: string
  - `chargingPointName`: string (sử dụng pointId)
  - `startSocPercent`: integer
  - `endSocPercent`: integer
  - `energyKwh`: number (float)
  - `costTotal`: number (float)
  - `status`: string (`IN_PROGRESS` | `COMPLETED` | `CANCELLED`)
  - `vehicleModel`: string (tên model dễ đọc)
  - `licensePlate`: string

## 4) Cách tính chi phí
- Khi phiên sạc kết thúc (COMPLETED hoặc CANCELLED), hệ thống tính chi phí:
  - Tìm plan mặc định "Linh hoạt" (PAY_AS_YOU_GO)
  - Công thức: `cost = (energyKwh × pricePerKwh) + (durationMin × pricePerMinute)`
  - Tạo bản ghi Payment với status PENDING
  - Driver thanh toán sau

## 5) Endpoints

### 5.1) Bắt đầu sạc
- `POST /api/charging-sessions/start`
- Quyền: `DRIVER` (Bearer token)
- Mô tả: Tạo phiên sạc mới trạng thái `IN_PROGRESS` tại trụ chỉ định, gắn vào xe của driver.
- Body (application/json):
```json
{
  "chargingPointId": "<your_point_id>",
  "vehicleId": "<your_vehicle_id>",
  "targetSocPercent": 90
}
```
- Validate và lỗi có thể trả về:
  - `CHARGING_POINT_NOT_FOUND`: không tìm thấy trụ
  - `CHARGING_POINT_NOT_AVAILABLE`: trụ không ở trạng thái AVAILABLE
  - `VEHICLE_NOT_FOUND`: không tìm thấy xe
  - `VEHICLE_NOT_BELONG_TO_DRIVER`: xe không thuộc quyền sở hữu driver gọi API
  - `INVALID_SOC_RANGE`: SOC hiện tại của xe đã >= target
- Kết quả: Trả về `ChargingSessionResponse` của phiên vừa tạo.

### 5.2) Dừng sạc thủ công
- `POST /api/charging-sessions/{sessionId}/stop`
- Quyền: `DRIVER` (Bearer token)
- Mô tả: Dừng phiên sạc đang hoạt động do người dùng chủ động. Trạng thái kết thúc là `CANCELLED`.
- Validate và lỗi:
  - `CHARGING_SESSION_NOT_FOUND`: không tìm thấy phiên
  - `UNAUTHORIZED`: phiên không thuộc driver gọi API
  - `CHARGING_SESSION_NOT_ACTIVE`: phiên không còn ở trạng thái `IN_PROGRESS`
- Kết quả: Thực hiện stop logic (cập nhật trạng thái, endTime, tính chi phí, giải phóng trụ, tạo Payment `PENDING`). Trả về `ChargingSessionResponse` sau cập nhật.

### 5.3) Xem chi tiết phiên sạc
- `GET /api/charging-sessions/{sessionId}`
- Quyền: `DRIVER` (Bearer token)
- Mô tả: Trả về thông tin chi tiết phiên sạc. Nếu phiên đang IN_PROGRESS, các giá trị `durationMin`, `endSocPercent`, `energyKwh` sẽ phản ánh tiến trình đã mô phỏng đến thời điểm gọi.
- Validate: `UNAUTHORIZED` nếu phiên không thuộc driver.
- Kết quả: `ChargingSessionResponse`.

### 5.4) Lịch sử các phiên sạc của tôi
- `GET /api/charging-sessions/my-sessions`
- Quyền: `DRIVER` (Bearer token)
- Mô tả: Trả về danh sách phiên sạc của driver, sắp xếp mới nhất trước.
- Trường hợp chưa có dữ liệu: trả về `200 OK` với mảng rỗng `[]` (không phải lỗi) để FE hiển thị trạng thái "chưa có phiên sạc".

## 6) Ví dụ cURL (Windows cmd)

Thiết lập biến môi trường tạm:
```cmd
set BASE_URL=http://localhost:8080
set TOKEN=<JWT_DRIVER>
set POINT_ID=<your_point_id>
set VEHICLE_ID=<your_vehicle_id>
```

### 6.1) Start phiên sạc
```cmd
curl -X POST "%BASE_URL%/api/charging-sessions/start" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"chargingPointId\":\"%POINT_ID%\",\"vehicleId\":\"%VEHICLE_ID%\",\"targetSocPercent\":90}"
```
Lưu `sessionId` trong phản hồi để dùng tiếp.

### 6.2) Kiểm tra tiến trình (poll mỗi 2–4 giây)
```cmd
set SESSION_ID=<your_session_id>
curl -X GET "%BASE_URL%/api/charging-sessions/%SESSION_ID%" -H "Authorization: Bearer %TOKEN%"
```
Kỳ vọng: `durationMin` tăng +2 mỗi ~2 giây; `energyKwh` và `endSocPercent` tăng tương ứng.

### 6.3) Dừng thủ công
```cmd
curl -X POST "%BASE_URL%/api/charging-sessions/%SESSION_ID%/stop" -H "Authorization: Bearer %TOKEN%"
```
Kết quả: phiên chuyển `CANCELLED`, tính chi phí đến thời điểm dừng, giải phóng trụ sạc.

### 6.4) Xem lịch sử các phiên sạc của tôi
```cmd
curl -X GET "%BASE_URL%/api/charging-sessions/my-sessions" -H "Authorization: Bearer %TOKEN%"
```

## 7) Mã lỗi tiêu biểu (ErrorCode)
- `CHARGING_POINT_NOT_AVAILABLE`
- `VEHICLE_NOT_BELONG_TO_DRIVER`
- `INVALID_SOC_RANGE`
- `CHARGING_SESSION_NOT_FOUND`
- `CHARGING_SESSION_NOT_ACTIVE`
- `UNAUTHORIZED`
- `USER_NOT_FOUND` / `DRIVER_NOT_FOUND`
- `CHARGING_POINT_NOT_FOUND` / `VEHICLE_NOT_FOUND`

## 8) Ghi chú
- Bạn có thể điều chỉnh tốc độ mô phỏng bằng cách đổi giá trị `fixedRate` trong `ChargingSimulatorService.simulateChargingTick()`.
- Nếu muốn cấu hình qua file, có thể chuyển `fixedRate` sang property (ví dụ: `app.simulator.fixedRateMs`) và dùng `@Scheduled(fixedRateString = "${app.simulator.fixedRateMs:2000}")`.
- Khi DB không có dữ liệu, các API collection trả về `[]` thay vì lỗi 500 để FE hiển thị "chưa có dữ liệu".
