# Fix Charging Session Bugs - November 8, 2025

## Các lỗi đã phát hiện và sửa

### 1. Lỗi thời gian sạc chạy rất lâu
**Nguyên nhân:** 
- Trong `ChargingSimulatorService.simulateChargingTick()`, mỗi tick (1 giây) chỉ cộng thêm `1/60` phút vào duration
- Điều này làm cho thời gian sạc thực tế chậm hơn 60 lần so với mong đợi

**Giải pháp:**
- Thay đổi logic simulation từ "1 giây = 1 giây thực" sang "1 tick = 1 phút sạc"
- Cập nhật `timePerTickMinutes = 1.0f` để mỗi tick tương đương 1 phút sạc
- Tính toán năng lượng và SOC tương ứng: `energyPerTick = powerKw * (1/60)` (năng lượng trong 1 phút)

### 2. Lỗi không có năng lượng (energyKwh = 0)
**Nguyên nhân:**
- Trong `convertToResponse()` có dòng `.energyKwh(session.getEnergyKwh() * 2)` - nhân đôi năng lượng vô lý
- Logic khởi tạo `endSocPercent` trong simulator không chính xác
- Không cập nhật `endSocPercent` từ vehicle trước khi dừng session

**Giải pháp:**
- Xóa phép nhân `* 2`, chỉ dùng giá trị thực: `.energyKwh(session.getEnergyKwh())`
- Sửa logic khởi tạo trong simulator: kiểm tra `startSocPercent` trước khi gán
- Thêm logic cập nhật `endSocPercent` từ vehicle trước khi gọi `stopSessionLogic()`

### 3. Lỗi không có tiền (costTotal = 0)
**Nguyên nhân:**
- Hàm `stopSessionLogic()` tính cost đúng nhưng thiếu logging để debug
- Có thể plan "Linh hoạt" không tồn tại trong database
- Công thức tính cost thiếu phần `pricePerMinute`

**Giải pháp:**
- Thêm logging chi tiết trong `stopSessionLogic()` để hiển thị công thức tính cost
- Cập nhật công thức: `cost = (energyKwh * pricePerKwh) + (durationMin * pricePerMinute)`
- Đảm bảo plan được load đúng với fallback values
- Thêm log khi không tìm thấy plan

### 4. Lỗi không thể dừng phiên sạc
**Nguyên nhân:**
- Logic `stopSessionLogic()` có thể fail im lặng nếu session không ở trạng thái IN_PROGRESS
- Không cập nhật vehicle SOC trước khi dừng
- Thiếu logging để debug

**Giải pháp:**
- Thêm validation và logging rõ ràng trong `stopSessionLogic()`
- Cập nhật `endSocPercent` từ vehicle's `currentSocPercent` trước khi gọi stop
- Sử dụng `saveAndFlush()` thay vì `save()` để đảm bảo changes được persist ngay
- Thêm log chi tiết về duration, energy, cost khi session stop

### 5. Cải thiện tính toán realtime trong convertToResponse()
**Vấn đề:**
- Chỉ tính cost dựa trên energy, không tính thêm phần thời gian
- Không load `pricePerMinute` từ plan

**Giải pháp:**
- Load cả `pricePerKwh` và `pricePerMinute` từ plan
- Tính `currentCost = (energyConsumed * pricePerKwh) + (elapsedMinutes * pricePerMinute)`
- Áp dụng cho cả IN_PROGRESS và COMPLETED sessions

## Các file đã sửa

1. **ChargingSimulatorService.java**
   - `simulateChargingTick()`: Sửa timing từ 1 giây -> 1 phút per tick
   - `stopSessionLogic()`: Thêm logging, sửa cost calculation, cập nhật vehicle SOC

2. **ChargingSessionService.java**
   - `convertToResponse()`: Xóa nhân đôi energy, sửa cost calculation
   - `startSession()`: Thêm logging và validation
   - `stopSessionByUser()`: Thêm cập nhật endSocPercent từ vehicle
   - `stopMyStationSession()`: Thêm cập nhật endSocPercent từ vehicle

## Testing Checklist

- [ ] Khởi tạo phiên sạc mới với SOC ban đầu đúng
- [ ] Quan sát realtime: SOC tăng dần theo thời gian
- [ ] Quan sát realtime: Energy tích lũy đúng
- [ ] Quan sát realtime: Cost tăng dần (bao gồm cả energy và time)
- [ ] Quan sát realtime: Duration tăng đúng (mỗi giây thực = 1 phút sạc)
- [ ] Dừng phiên sạc thủ công: cost, energy, duration được tính đúng
- [ ] Sạc đến đích (auto stop): tương tự trên
- [ ] Payment record được tạo tự động với status UNPAID
- [ ] Vehicle SOC được cập nhật sau khi kết thúc
- [ ] Charging point được release (status = AVAILABLE)

## Lưu ý

### Tốc độ simulation
- Hiện tại: 1 tick (1 giây thực) = 1 phút sạc
- Điều này giúp test nhanh hơn nhưng vẫn realistic
- Nếu cần simulation chậm hơn, có thể điều chỉnh `timePerTickMinutes` hoặc `fixedRate`

### Plan pricing
- Default plan: "Linh hoạt"
- Nếu không tìm thấy plan, sử dụng fallback: 3800 VND/kWh, 0 VND/minute
- Đảm bảo plan tồn tại trong database cho kết quả chính xác

### Logging
- Đã thêm extensive logging để debug
- Check console/logs khi có issue để trace root cause

