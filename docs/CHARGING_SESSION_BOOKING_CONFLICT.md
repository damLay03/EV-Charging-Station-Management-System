# Phân Tích: Xung Đột Khi Phiên Sạc Kéo Dài Qua Giờ Booking

## 📋 Tổng Quan

Document này phân tích chi tiết vấn đề xung đột xảy ra khi một phiên sạc kéo dài vượt quá thời gian dự kiến và chồng lấn với booking tiếp theo.

**Ngày tạo:** 22/11/2025  
**Mức độ nghiêm trọng:** 🔴 **CAO**  
**Tác động:** Trải nghiệm người dùng, Revenue loss, Hệ thống booking

---

## 🔴 Vấn Đề Hiện Tại

### Mô Tả Ngắn Gọn

Hệ thống hiện tại **KHÔNG kiểm tra phiên sạc đang chạy** khi tạo booking mới, chỉ kiểm tra booking conflict. Điều này dẫn đến tình huống:

- Người A đang sạc (không có booking)
- Người B có thể đặt booking cho cùng trụ đó
- Khi đến giờ booking của B, người A vẫn đang sạc
- **Xung đột không thể giải quyết!**

---

## 📊 Các Kịch Bản Xung Đột

### Kịch Bản 1: Phiên Sạc Thường Kéo Dài

```
Timeline:
┌─────────────────────────────────────────────────────────────┐
│ 10:00        10:15        10:30        10:45        11:00   │
│   │           │            │            │            │      │
│   A START     B BOOK       [A dự kiến   B's TIME     A END  │
│   CHARGING    (10:45)       xong]                            │
│   │           │            │            │            │      │
│   ├───────────┴────────────┴────────────┴────────────┤      │
│   │         Người A vẫn đang sạc (session kéo dài)   │      │
│   └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘

Status:
10:00 → ChargingPoint: CHARGING, currentSession: #123
10:15 → Booking B được tạo thành công ✅ (KHÔNG kiểm tra session!)
10:30 → ChargingPoint: vẫn CHARGING (A chưa xong)
10:45 → B đến check-in → ❌ KHÔNG THỂ SỬ DỤNG!
        ChargingPoint vẫn CHARGING, A vẫn đang sạc
11:00 → A kết thúc, nhưng B đã mất 15 phút chờ đợi
```

**Vấn đề:**
- ❌ B đã trả tiền đặt cọc nhưng không thể sử dụng đúng giờ
- ❌ Không có cơ chế thông báo cho B rằng trụ vẫn đang bận
- ❌ Không có compensation cho B
- ❌ A không biết là mình đang block người khác

---

### Kịch Bản 2: Booking Chồng Lấn Booking

```
Timeline:
┌─────────────────────────────────────────────────────────────┐
│ 10:00   10:15   10:30   10:45   11:00   11:15   11:30      │
│   │       │       │       │       │       │       │         │
│   A       │       [A dự   B       │       │       A         │
│   BOOK    B BOOK  kiến]  BOOK     │       │       ACTUAL    │
│   +START  (10:45) END    TIME     │       │       END       │
│   │       │       │       │       │       │       │         │
│   ├───────┴───────┴───────┴───────┴───────┴───────┤         │
│   │    A's session kéo dài (xe có pin lớn hơn)    │         │
│   └────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────┘

Phân tích:
1. A có booking 10:00, dự kiến sạc 30 phút (đến 10:30)
   - Input: 50% → 80% (30% charge)
   - Dự kiến: 30 phút với 60kW charger
   
2. Hệ thống tính: 10:30 → 10:45 có 15 phút trống → Cho phép B booking ✅

3. Thực tế: A's vehicle battery = 100kWh (lớn hơn DB: 75kWh)
   - Actual charging time: 50 phút
   - A sạc xong lúc 10:50
   
4. B đến lúc 10:45 → ❌ Trụ vẫn CHARGING
```

**Root Cause:**
- ❌ Tính toán thời gian sạc dựa trên lý thuyết, không có buffer
- ❌ Không có khoảng trống (gap) giữa các booking
- ❌ Không tính đến charging curve (sạc chậm dần khi gần đầy)

---

### Kịch Bản 3: Walk-in User Chặn Booking

```
Timeline:
┌─────────────────────────────────────────────────────────────┐
│ 09:00        10:00        10:30        11:00               │
│   │           │            │            │                   │
│   B BOOK      A WALK-IN    B's BOOKING  A ACTUAL END       │
│   (10:30)     START        TIME         (nếu không bị gián │
│   │           CHARGING     │            đoạn)              │
│   │           │            │            │                   │
│   │           ├────────────┴────────────┤                   │
│   │           │   A sạc không có booking│                   │
│   └───────────┴─────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘

Vấn đề:
- B đặt trước từ 09:00 cho slot 10:30
- A đến lúc 10:00, trụ đang AVAILABLE → được phép sạc
- Hệ thống KHÔNG kiểm tra "có booking sau 30 phút không?"
- A sạc thoải mái đến 11:00
- B đến 10:30 → không thể dùng
```

**Root Cause:**
- ❌ Khi start session, KHÔNG kiểm tra upcoming bookings
- ❌ KHÔNG giới hạn max charging time nếu có booking sau
- ❌ KHÔNG warning user về booking tiếp theo

---

## 🔍 Phân Tích Code Hiện Tại

### File: `BookingService.java`

#### ❌ Vấn đề 1: `checkAvailability()` không kiểm tra active session

```java
public BookingAvailabilityDto checkAvailability(...) {
    // ... code ...
    
    // ✅ Kiểm tra booking conflict
    Optional<Booking> conflicting = bookingRepository.findConflictingBooking(
        chargingPointId, startTime, endTime);
    
    if (conflicting.isPresent()) {
        return BookingAvailabilityDto.builder()
            .available(false)
            .message("Trụ đã được đặt trong khung giờ này")
            .build();
    }
    
    // ❌ THIẾU: Không kiểm tra active charging session!
    // ChargingPoint có thể đang CHARGING với session kết thúc sau bookingTime
}
```

**Cần thêm:**
```java
// Kiểm tra xem có session đang chạy không
ChargingSession activeSession = chargingPoint.getCurrentSession();
if (activeSession != null && 
    activeSession.getStatus() == ChargingSessionStatus.IN_PROGRESS) {
    
    // Ước tính thời gian kết thúc session
    LocalDateTime estimatedEndTime = estimateSessionEndTime(activeSession);
    
    if (estimatedEndTime.isAfter(bookingTime)) {
        return BookingAvailabilityDto.builder()
            .available(false)
            .message("Trụ hiện đang có phiên sạc dự kiến kết thúc lúc " + estimatedEndTime)
            .build();
    }
}
```

---

#### ❌ Vấn đề 2: Không có buffer time giữa bookings

```java
// Tính thời gian có thể sạc dựa trên booking tiếp theo
if (nextBookingOpt.isPresent()) {
    Booking nextBooking = nextBookingOpt.get();
    Duration timeSlot = Duration.between(bookingTime, nextBooking.getBookingTime());
    // ❌ Sử dụng TOÀN BỘ thời gian, không có buffer!
    double availableEnergy = (chargingPoint.getChargingPower().getPowerKw() / 1000.0) 
                            * (timeSlot.toMinutes() / 60.0);
}
```

**Nên thêm buffer:**
```java
private static final int BUFFER_BETWEEN_BOOKINGS_MINUTES = 15;

if (nextBookingOpt.isPresent()) {
    Booking nextBooking = nextBookingOpt.get();
    Duration timeSlot = Duration.between(bookingTime, nextBooking.getBookingTime());
    
    // ✅ Trừ đi buffer time
    long availableMinutes = timeSlot.toMinutes() - BUFFER_BETWEEN_BOOKINGS_MINUTES;
    
    if (availableMinutes <= 0) {
        return BookingAvailabilityDto.builder()
            .available(false)
            .message("Không đủ thời gian giữa các booking")
            .build();
    }
    
    double availableEnergy = (chargingPoint.getChargingPower().getPowerKw() / 1000.0) 
                            * (availableMinutes / 60.0);
}
```

---

### File: `ChargingSessionService.java`

#### ❌ Vấn đề 3: Không kiểm tra upcoming booking khi start session

```java
@Transactional
public ChargingSessionResponse startChargingSession(...) {
    // ... validation code ...
    
    // ❌ THIẾU: Không kiểm tra có booking sắp tới không
    // Nếu có booking sau 30 phút, nên warning hoặc limit max charging time
    
    ChargingSession newSession = ChargingSession.builder()
        .status(ChargingSessionStatus.IN_PROGRESS)
        .build();
    
    chargingPoint.setStatus(ChargingPointStatus.CHARGING);
}
```

**Nên thêm:**
```java
// Kiểm tra upcoming bookings
List<Booking> upcomingBookings = bookingRepository.findUpcomingBookingsForPoint(
    chargingPoint.getPointId(), 
    LocalDateTime.now(), 
    LocalDateTime.now().plusHours(2)
);

if (!upcomingBookings.isEmpty()) {
    Booking nextBooking = upcomingBookings.get(0);
    Duration timeUntilBooking = Duration.between(LocalDateTime.now(), nextBooking.getBookingTime());
    
    if (timeUntilBooking.toMinutes() < 60) {
        // Warning: Có booking sau ít hơn 1 giờ
        log.warn("Starting session with upcoming booking in {} minutes", timeUntilBooking.toMinutes());
        
        // Option 1: Reject session start
        // throw new AppException(ErrorCode.CHARGING_POINT_RESERVED);
        
        // Option 2: Set max charging time
        newSession.setMaxChargingMinutes(timeUntilBooking.toMinutes() - 15);
        
        // Option 3: Warning only
        // (current behavior)
    }
}
```

---

### File: `ChargingSimulatorService.java`

#### ❌ Vấn đề 4: Không có auto-terminate khi đến giờ booking

```java
@Scheduled(fixedDelay = 30000) // Chạy mỗi 30 giây
@Transactional
public void simulateCharging() {
    List<ChargingSession> activeSessions = 
        chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);
    
    for (ChargingSession session : activeSessions) {
        // ... update SOC ...
        
        if (currentSoc >= session.getTargetSoc()) {
            // Auto complete
            completeSession(session);
        }
        
        // ❌ THIẾU: Không kiểm tra có booking sắp tới không
    }
}
```

**Nên thêm:**
```java
// Kiểm tra xem có booking sắp đến không
List<Booking> upcomingBookings = bookingRepository.findUpcomingBookingsForPoint(
    session.getChargingPoint().getPointId(),
    LocalDateTime.now(),
    LocalDateTime.now().plusMinutes(10)
);

if (!upcomingBookings.isEmpty()) {
    Booking nextBooking = upcomingBookings.get(0);
    LocalDateTime bookingTime = nextBooking.getBookingTime();
    LocalDateTime now = LocalDateTime.now();
    
    if (now.isAfter(bookingTime.minusMinutes(10))) {
        // 10 phút trước giờ booking → Warning
        log.warn("Session {} is approaching booking time {}", 
                 session.getSessionId(), bookingTime);
        
        // Gửi notification cho user
        notificationService.sendUpcomingBookingWarning(session, nextBooking);
    }
    
    if (now.isAfter(bookingTime.minusMinutes(5))) {
        // 5 phút trước giờ booking → Force complete
        log.warn("Force completing session {} due to upcoming booking", 
                 session.getSessionId());
        
        session.setForceStoppedReason("Upcoming booking at " + bookingTime);
        completeSession(session);
    }
}
```

---

## 💡 Giải Pháp Đề Xuất

### Solution 1: ✅ Kiểm Tra Active Session Khi Tạo Booking (RECOMMENDED)

**Mức độ ưu tiên:** 🔴 CAO  
**Độ phức tạp:** Trung bình  
**Impact:** Giải quyết gốc rễ vấn đề

#### Implementation:

**File: `BookingService.java`**

```java
@Transactional
public BookingResponse createBooking(BookingRequest request, String email) {
    // ... existing validation ...
    
    ChargingPoint chargingPoint = chargingPointRepository
        .findById(request.getChargingPointId())
        .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
    
    // ✅ THÊM: Kiểm tra active session
    ChargingSession activeSession = chargingPoint.getCurrentSession();
    if (activeSession != null && 
        activeSession.getStatus() == ChargingSessionStatus.IN_PROGRESS) {
        
        // Ước tính thời gian kết thúc session
        LocalDateTime estimatedEndTime = calculateEstimatedEndTime(activeSession);
        
        // Thêm buffer 15 phút
        LocalDateTime safeAvailableTime = estimatedEndTime.plusMinutes(15);
        
        if (safeAvailableTime.isAfter(request.getBookingTime())) {
            throw new AppException(ErrorCode.CHARGING_POINT_BUSY)
                .withMessage("Trụ hiện đang có phiên sạc, dự kiến kết thúc lúc " + 
                            estimatedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
    
    // ... continue with booking creation ...
}

/**
 * Tính toán thời gian dự kiến kết thúc session dựa trên:
 * - SOC hiện tại
 * - Target SOC
 * - Charging power
 * - Battery capacity
 * - Safety margin (thêm 20% thời gian dự phòng)
 */
private LocalDateTime calculateEstimatedEndTime(ChargingSession session) {
    double currentSoc = session.getCurrentSoc();
    double targetSoc = session.getTargetSoc();
    double remainingPercent = targetSoc - currentSoc;
    
    Vehicle vehicle = session.getVehicle();
    ChargingPoint point = session.getChargingPoint();
    
    double requiredEnergy = (remainingPercent / 100.0) * vehicle.getBatteryCapacityKwh();
    double chargingPowerKw = point.getChargingPower().getPowerKw() / 1000.0;
    
    // Tính thời gian lý thuyết
    double hoursNeeded = requiredEnergy / chargingPowerKw;
    
    // Thêm 20% safety margin (charging curve, temperature, etc.)
    double safetyFactor = 1.2;
    double adjustedHours = hoursNeeded * safetyFactor;
    
    long minutesNeeded = (long) (adjustedHours * 60);
    
    return LocalDateTime.now().plusMinutes(minutesNeeded);
}
```

**Pros:**
- ✅ Ngăn chặn xung đột ngay từ đầu
- ✅ User experience tốt: Biết rõ lý do không thể book
- ✅ Không cần thay đổi flow hiện tại

**Cons:**
- ⚠️ Có thể reject booking hợp lệ nếu ước tính sai
- ⚠️ Giảm availability của trụ

---

### Solution 2: ✅ Thêm Buffer Time Bắt Buộc Giữa Bookings

**Mức độ ưu tiên:** 🔴 CAO  
**Độ phức tạp:** Thấp  
**Impact:** Giảm conflict, tăng flexibility

#### Implementation:

```java
private static final int BUFFER_BETWEEN_BOOKINGS_MINUTES = 15;
private static final int MIN_BOOKING_DURATION_MINUTES = 15;

public BookingAvailabilityDto checkAvailability(
    String chargingPointId, 
    LocalDateTime bookingTime, 
    Long vehicleId
) {
    // ... existing code ...
    
    // Tìm booking TRƯỚC đó
    Optional<Booking> previousBooking = bookingRepository
        .findLastBookingBefore(chargingPointId, bookingTime);
    
    if (previousBooking.isPresent()) {
        LocalDateTime prevEndTime = calculateBookingEndTime(previousBooking.get());
        LocalDateTime minStartTime = prevEndTime.plusMinutes(BUFFER_BETWEEN_BOOKINGS_MINUTES);
        
        if (bookingTime.isBefore(minStartTime)) {
            return BookingAvailabilityDto.builder()
                .available(false)
                .message(String.format("Cần buffer %d phút sau booking trước. " +
                                      "Thời gian sớm nhất: %s", 
                                      BUFFER_BETWEEN_BOOKINGS_MINUTES,
                                      minStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))))
                .earliestAvailableTime(minStartTime)
                .build();
        }
    }
    
    // Tìm booking SAU đó
    Optional<Booking> nextBooking = bookingRepository
        .findNextBookingAfter(chargingPointId, bookingTime);
    
    if (nextBooking.isPresent()) {
        LocalDateTime nextStartTime = nextBooking.get().getBookingTime();
        Duration availableTime = Duration.between(bookingTime, nextStartTime);
        
        long maxMinutes = availableTime.toMinutes() - BUFFER_BETWEEN_BOOKINGS_MINUTES;
        
        if (maxMinutes < MIN_BOOKING_DURATION_MINUTES) {
            return BookingAvailabilityDto.builder()
                .available(false)
                .message("Không đủ thời gian giữa các booking")
                .build();
        }
        
        // Tính max charge percentage với thời gian có sẵn
        double maxChargePercentage = calculateMaxCharge(
            chargingPoint, vehicle, maxMinutes);
        
        return BookingAvailabilityDto.builder()
            .available(true)
            .maxChargePercentage(maxChargePercentage)
            .maxChargingMinutes(maxMinutes)
            .message(String.format("Bạn có tối đa %d phút sạc (buffer 15 phút)", maxMinutes))
            .build();
    }
    
    // ... continue ...
}
```

**Update DTO:**

```java
@Data
@Builder
public class BookingAvailabilityDto {
    private boolean available;
    private double maxChargePercentage;
    private Long maxChargingMinutes;  // ✅ THÊM
    private LocalDateTime earliestAvailableTime;  // ✅ THÊM
    private String message;
}
```

**Pros:**
- ✅ Đơn giản, dễ hiểu
- ✅ Tạo khoảng trống cho cleanup, unplugging
- ✅ Giảm stress trong vận hành

**Cons:**
- ⚠️ Giảm 15 phút capacity mỗi slot
- ⚠️ Có thể gây waste time nếu user kết thúc sớm

---

### Solution 3: ⚡ Auto-Terminate Session Trước Booking (CRITICAL)

**Mức độ ưu tiên:** 🔴 CAO  
**Độ phức tạp:** Cao  
**Impact:** Đảm bảo booking được tôn trọng

#### Implementation:

**File: `ChargingSimulatorService.java`**

```java
@Scheduled(fixedDelay = 30000) // Mỗi 30 giây
@Transactional
public void simulateCharging() {
    List<ChargingSession> activeSessions = 
        chargingSessionRepository.findByStatus(ChargingSessionStatus.IN_PROGRESS);
    
    for (ChargingSession session : activeSessions) {
        // ✅ THÊM: Kiểm tra upcoming booking
        if (shouldTerminateForUpcomingBooking(session)) {
            terminateSessionForBooking(session);
            continue;
        }
        
        // ... existing charging simulation ...
    }
}

/**
 * Kiểm tra xem có nên terminate session vì có booking sắp tới
 */
private boolean shouldTerminateForUpcomingBooking(ChargingSession session) {
    ChargingPoint point = session.getChargingPoint();
    LocalDateTime now = LocalDateTime.now();
    
    // Tìm booking sắp tới trong 10 phút
    List<Booking> upcomingBookings = bookingRepository
        .findUpcomingBookingsForPoint(
            point.getPointId(),
            now,
            now.plusMinutes(10)
        );
    
    if (upcomingBookings.isEmpty()) {
        return false;
    }
    
    Booking nextBooking = upcomingBookings.get(0);
    
    // Nếu có booking từ người khác (không phải user đang sạc)
    if (!nextBooking.getUser().getUserId().equals(
            session.getStartedByUser().getUserId())) {
        
        LocalDateTime bookingTime = nextBooking.getBookingTime();
        
        // Terminate 5 phút trước giờ booking
        if (now.isAfter(bookingTime.minusMinutes(5))) {
            log.warn("Session {} must terminate for upcoming booking at {}", 
                     session.getSessionId(), bookingTime);
            return true;
        }
        
        // Warning 10 phút trước
        if (now.isAfter(bookingTime.minusMinutes(10))) {
            sendUpcomingBookingWarning(session, nextBooking);
        }
    }
    
    return false;
}

/**
 * Terminate session vì có booking sắp tới
 */
private void terminateSessionForBooking(ChargingSession session) {
    ChargingPoint point = session.getChargingPoint();
    
    // Tìm booking
    Booking nextBooking = bookingRepository
        .findUpcomingBookingsForPoint(
            point.getPointId(),
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(10)
        ).get(0);
    
    // Complete session
    session.setEndTime(LocalDateTime.now());
    session.setStatus(ChargingSessionStatus.FORCE_STOPPED);
    session.setStopReason(String.format(
        "Auto-terminated for booking #%d at %s", 
        nextBooking.getId(),
        nextBooking.getBookingTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    ));
    
    // Calculate cost
    double energyConsumed = calculateEnergyConsumed(session);
    double cost = calculateCost(session, energyConsumed);
    
    session.setEnergyConsumed(energyConsumed);
    session.setTotalCost(cost);
    
    chargingSessionRepository.save(session);
    
    // Update charging point
    point.setStatus(ChargingPointStatus.AVAILABLE);
    point.setCurrentSession(null);
    chargingPointRepository.save(point);
    
    // Process payment
    paymentService.createPaymentForSession(session);
    
    // Send notification
    notificationService.sendSessionTerminatedNotification(
        session, 
        "Phiên sạc đã được tự động kết thúc vì có booking tiếp theo"
    );
    
    log.info("Session {} force stopped for upcoming booking #{}", 
             session.getSessionId(), nextBooking.getId());
}

/**
 * Gửi cảnh báo cho user về booking sắp tới
 */
private void sendUpcomingBookingWarning(ChargingSession session, Booking nextBooking) {
    // Chỉ gửi 1 lần
    if (session.getWarningNotificationSent() != null && 
        session.getWarningNotificationSent()) {
        return;
    }
    
    LocalDateTime bookingTime = nextBooking.getBookingTime();
    Duration timeRemaining = Duration.between(LocalDateTime.now(), bookingTime);
    
    notificationService.sendInAppNotification(
        session.getStartedByUser(),
        "Cảnh báo: Phiên sạc sắp kết thúc",
        String.format("Có booking tiếp theo sau %d phút. Vui lòng hoàn tất sạc trước %s",
                     timeRemaining.toMinutes(),
                     bookingTime.format(DateTimeFormatter.ofPattern("HH:mm")))
    );
    
    session.setWarningNotificationSent(true);
    chargingSessionRepository.save(session);
}
```

**Database Changes:**

```sql
-- Thêm field tracking warning
ALTER TABLE charging_sessions 
ADD COLUMN warning_notification_sent BOOLEAN DEFAULT FALSE;

ALTER TABLE charging_sessions
ADD COLUMN stop_reason VARCHAR(500);
```

**Thêm status mới:**

```java
public enum ChargingSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FORCE_STOPPED  // ✅ THÊM: Bị dừng tự động vì booking
}
```

**Pros:**
- ✅ Đảm bảo booking được ưu tiên
- ✅ Tự động hóa, không cần staff can thiệp
- ✅ Fair cho người có booking

**Cons:**
- ⚠️ User experience không tốt nếu bị force stop
- ⚠️ Có thể gây khó chịu cho người đang sạc
- ⚠️ Cần policy rõ ràng về compensation

---

### Solution 4: 📊 Real-time Session Time Estimation

**Mức độ ưu tiên:** 🟡 TRUNG BÌNH  
**Độ phức tạp:** Cao  
**Impact:** Cải thiện accuracy của time estimate

#### Implementation:

```java
/**
 * Service ước tính thời gian sạc chính xác hơn
 */
@Service
@RequiredArgsConstructor
public class ChargingTimeEstimationService {
    
    /**
     * Ước tính thời gian sạc dựa trên charging curve thực tế
     * 
     * Charging curve điển hình:
     * - 0-80%: Full power (100% rated power)
     * - 80-90%: 70% power
     * - 90-95%: 50% power
     * - 95-100%: 30% power
     */
    public ChargingTimeEstimate estimateChargingTime(
        Vehicle vehicle,
        ChargingPower chargingPower,
        double fromSoc,
        double toSoc
    ) {
        double batteryCapacity = vehicle.getBatteryCapacityKwh();
        double maxPowerKw = chargingPower.getPowerKw() / 1000.0;
        
        // Account for battery degradation
        double degradationFactor = calculateDegradationFactor(vehicle);
        batteryCapacity = batteryCapacity * degradationFactor;
        
        double totalMinutes = 0;
        
        // Tính từng segment với power khác nhau
        List<ChargingSegment> segments = calculateSegments(fromSoc, toSoc);
        
        for (ChargingSegment segment : segments) {
            double powerFactor = segment.getPowerFactor();
            double effectivePower = maxPowerKw * powerFactor;
            
            double energyNeeded = batteryCapacity * 
                (segment.getToSoc() - segment.getFromSoc()) / 100.0;
            
            double hours = energyNeeded / effectivePower;
            totalMinutes += hours * 60;
        }
        
        // Thêm overhead (connection time, handshake, etc.)
        totalMinutes += 2;
        
        // Thêm temperature adjustment
        double tempFactor = getTemperatureAdjustmentFactor();
        totalMinutes *= tempFactor;
        
        // Safety margin
        double minEstimate = totalMinutes;
        double maxEstimate = totalMinutes * 1.3; // +30% buffer
        
        return ChargingTimeEstimate.builder()
            .estimatedMinutes((long) totalMinutes)
            .minMinutes((long) minEstimate)
            .maxMinutes((long) maxEstimate)
            .confidenceLevel(calculateConfidence(vehicle))
            .build();
    }
    
    private List<ChargingSegment> calculateSegments(double fromSoc, double toSoc) {
        List<ChargingSegment> segments = new ArrayList<>();
        
        if (fromSoc < 80 && toSoc > fromSoc) {
            double segmentEnd = Math.min(80, toSoc);
            segments.add(new ChargingSegment(fromSoc, segmentEnd, 1.0)); // 100% power
        }
        
        if (fromSoc < 90 && toSoc > 80) {
            double segmentStart = Math.max(80, fromSoc);
            double segmentEnd = Math.min(90, toSoc);
            segments.add(new ChargingSegment(segmentStart, segmentEnd, 0.7)); // 70% power
        }
        
        if (fromSoc < 95 && toSoc > 90) {
            double segmentStart = Math.max(90, fromSoc);
            double segmentEnd = Math.min(95, toSoc);
            segments.add(new ChargingSegment(segmentStart, segmentEnd, 0.5)); // 50% power
        }
        
        if (toSoc > 95) {
            double segmentStart = Math.max(95, fromSoc);
            segments.add(new ChargingSegment(segmentStart, toSoc, 0.3)); // 30% power
        }
        
        return segments;
    }
    
    private double calculateDegradationFactor(Vehicle vehicle) {
        // Giả sử xe mới: 100%, mỗi năm giảm 2%
        int vehicleAge = calculateVehicleAge(vehicle);
        return Math.max(0.80, 1.0 - (vehicleAge * 0.02));
    }
    
    private double getTemperatureAdjustmentFactor() {
        // TODO: Integrate with weather API
        // Cold weather: 1.2x, Normal: 1.0x, Hot: 1.1x
        return 1.0;
    }
    
    private double calculateConfidence(Vehicle vehicle) {
        // Confidence based on data availability
        // New vehicle with no charging history: 0.6
        // Vehicle with 10+ sessions: 0.9
        // TODO: Implement based on historical data
        return 0.7;
    }
}

@Data
@Builder
class ChargingSegment {
    private double fromSoc;
    private double toSoc;
    private double powerFactor; // 0.0 - 1.0
}

@Data
@Builder
class ChargingTimeEstimate {
    private long estimatedMinutes;
    private long minMinutes;
    private long maxMinutes;
    private double confidenceLevel; // 0.0 - 1.0
}
```

**Pros:**
- ✅ Ước tính chính xác hơn nhiều
- ✅ Giảm conflict do sai lệch thời gian
- ✅ Có thể improve theo thời gian với ML

**Cons:**
- ⚠️ Phức tạp, khó maintain
- ⚠️ Cần data về charging curve của từng xe
- ⚠️ Vẫn không 100% accurate

---

### Solution 5: 🎯 Dynamic Slot Management với Overbooking

**Mức độ ưu tiên:** 🟢 THẤP (Future enhancement)  
**Độ phức tạp:** Rất cao  
**Impact:** Maximize utilization

#### Concept:

```java
/**
 * Cho phép overbook với risk management
 * Tương tự airline: Book nhiều hơn capacity thực tế
 */
@Service
public class DynamicSlotManagementService {
    
    private static final double OVERBOOKING_FACTOR = 1.1; // 10% overbook
    
    /**
     * Tính toán có nên accept booking hay không dựa trên:
     * - Historical completion time
     * - Current load
     * - User reliability score
     * - Compensation budget
     */
    public BookingDecision evaluateBookingRequest(
        ChargingPoint point,
        LocalDateTime requestedTime,
        User user
    ) {
        // Get historical data
        Statistics stats = getHistoricalStats(point);
        
        // Calculate risk
        double conflictProbability = calculateConflictProbability(
            point, requestedTime, stats);
        
        double expectedCompensationCost = conflictProbability * 
            DEPOSIT_AMOUNT * 2; // 2x refund nếu conflict
        
        double expectedRevenue = DEPOSIT_AMOUNT + 
            estimateAverageSessionRevenue(point);
        
        double expectedProfit = expectedRevenue - expectedCompensationCost;
        
        if (expectedProfit > 0) {
            return BookingDecision.ACCEPT;
        } else if (expectedProfit > -DEPOSIT_AMOUNT * 0.5) {
            return BookingDecision.ACCEPT_WITH_DISCOUNT;
        } else {
            return BookingDecision.REJECT;
        }
    }
}
```

**Pros:**
- ✅ Maximize revenue
- ✅ Reduce idle time
- ✅ Better resource utilization

**Cons:**
- ⚠️ Rất phức tạp
- ⚠️ Risk cao nếu không quản lý tốt
- ⚠️ Cần compensation policy rõ ràng
- ⚠️ Có thể gây mất lòng tin

---

## 📝 Recommendation: Phương Án Triển Khai

### Phase 1: Quick Fixes (1-2 tuần) ✅ PRIORITY

1. **✅ Thêm kiểm tra active session trong `checkAvailability()`**
   - Implementation: Solution 1
   - Effort: 2 ngày
   - Impact: Ngăn chặn 80% conflicts

2. **✅ Thêm buffer time 15 phút giữa bookings**
   - Implementation: Solution 2
   - Effort: 1 ngày
   - Impact: Tạo khoảng trống an toàn

3. **✅ Improve error messages**
   - Show estimated end time của session hiện tại
   - Show earliest available time
   - Effort: 1 ngày

### Phase 2: Auto-Management (2-3 tuần)

4. **⚡ Implement auto-terminate trước booking**
   - Implementation: Solution 3
   - Effort: 5 ngày
   - Impact: Đảm bảo booking được tôn trọng

5. **📧 Notification system**
   - Warning 10 phút trước
   - Email/SMS notification
   - In-app push notification
   - Effort: 3 ngày

6. **💰 Compensation policy**
   - Auto refund nếu không thể sử dụng đúng giờ
   - Discount cho booking tiếp theo
   - Effort: 2 ngày

### Phase 3: Optimization (1-2 tháng)

7. **📊 Better time estimation**
   - Implementation: Solution 4
   - Collect charging data
   - ML model cho prediction
   - Effort: 2-3 tuần

8. **📈 Analytics dashboard**
   - Track conflict rate
   - Monitor average session time
   - Identify problematic time slots
   - Effort: 1 tuần

### Phase 4: Advanced Features (Future)

9. **🎯 Dynamic slot management**
   - Implementation: Solution 5
   - Risk-based acceptance
   - Overbooking management
   - Effort: 1-2 tháng

---

## 🔧 Code Changes Required

### 1. Database Schema Changes

```sql
-- Thêm columns vào charging_sessions
ALTER TABLE charging_sessions
ADD COLUMN warning_notification_sent BOOLEAN DEFAULT FALSE,
ADD COLUMN stop_reason VARCHAR(500),
ADD COLUMN force_stopped_at TIMESTAMP,
ADD COLUMN max_charging_minutes INTEGER;

-- Thêm index cho performance
CREATE INDEX idx_charging_sessions_status_point 
ON charging_sessions(status, charging_point_id);

CREATE INDEX idx_bookings_point_time 
ON bookings(charging_point_id, booking_time);

-- Thêm bảng session history cho analytics
CREATE TABLE charging_session_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(255),
    event_type VARCHAR(50), -- START, WARNING, FORCE_STOP, COMPLETE
    event_time TIMESTAMP,
    reason TEXT,
    metadata JSON
);
```

### 2. New Repository Methods

```java
// BookingRepository.java
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // ✅ Tìm booking cuối cùng trước một thời điểm
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime < :time " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime DESC")
    Optional<Booking> findLastBookingBefore(
        @Param("pointId") String pointId,
        @Param("time") LocalDateTime time
    );
    
    // ✅ Tìm booking tiếp theo sau một thời điểm
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime > :time " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime ASC")
    Optional<Booking> findNextBookingAfter(
        @Param("pointId") String pointId,
        @Param("time") LocalDateTime time
    );
    
    // ✅ Tìm các booking sắp tới cho một trụ
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime BETWEEN :start AND :end " +
           "AND b.bookingStatus = 'CONFIRMED' " +
           "ORDER BY b.bookingTime ASC")
    List<Booking> findUpcomingBookingsForPoint(
        @Param("pointId") String pointId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
```

### 3. New DTOs

```java
@Data
@Builder
public class BookingAvailabilityDto {
    private boolean available;
    private double maxChargePercentage;
    private Long maxChargingMinutes;  // ✅ NEW
    private LocalDateTime earliestAvailableTime;  // ✅ NEW
    private SessionConflictInfo conflictInfo;  // ✅ NEW
    private String message;
}

@Data
@Builder
public class SessionConflictInfo {
    private boolean hasActiveSession;
    private String sessionId;
    private LocalDateTime estimatedEndTime;
    private double currentSoc;
    private double targetSoc;
}
```

### 4. New Error Codes

```java
public enum ErrorCode {
    // ... existing codes ...
    
    CHARGING_POINT_BUSY(4001, "Trụ sạc hiện đang bận", HttpStatus.CONFLICT),
    INSUFFICIENT_TIME_BETWEEN_BOOKINGS(4002, "Không đủ thời gian giữa các booking", HttpStatus.CONFLICT),
    SESSION_FORCE_STOPPED(4003, "Phiên sạc đã bị dừng tự động", HttpStatus.OK),
}
```

### 5. Configuration Properties

```yaml
# application.yaml

charging:
  booking:
    buffer-minutes: 15  # Buffer time giữa các booking
    min-duration-minutes: 15  # Thời gian booking tối thiểu
    max-duration-hours: 4  # Thời gian booking tối đa
    
  session:
    auto-terminate:
      enabled: true
      warning-minutes: 10  # Cảnh báo trước khi terminate
      force-stop-minutes: 5  # Force stop trước booking
      
  estimation:
    safety-margin-factor: 1.2  # Thêm 20% thời gian dự phòng
    confidence-threshold: 0.7  # Ngưỡng confidence để accept
```

---

## 📊 Testing Scenarios

### Test Case 1: Reject Booking khi có Active Session

```
Given:
  - ChargingPoint CP-001 đang có session active
  - Current SOC: 50%, Target: 80%
  - Estimated end time: 14:30
  - Buffer: 15 minutes → Safe time: 14:45

When:
  - User B tries to book CP-001 for 14:30

Then:
  - Booking rejected
  - Error message: "Trụ hiện đang có phiên sạc, dự kiến kết thúc lúc 14:30"
  - Suggest earliest time: 14:45
```

### Test Case 2: Force Stop Session Before Booking

```
Given:
  - Session #123 đang chạy từ 14:00
  - Booking #456 for 15:00
  - Current time: 14:50

When:
  - Simulator runs at 14:55 (5 minutes before booking)

Then:
  - Session #123 auto-stopped
  - Status: FORCE_STOPPED
  - Reason: "Auto-terminated for booking #456 at 15:00"
  - Notification sent to user
  - ChargingPoint status: AVAILABLE
```

### Test Case 3: Buffer Time Validation

```
Given:
  - Booking A: 14:00 - 14:30 (estimated)
  - Buffer: 15 minutes
  - Next available: 14:45

When:
  - User B tries to book for 14:35

Then:
  - Booking rejected
  - Error: "Cần buffer 15 phút sau booking trước"
  - Earliest available: 14:45
```

---

## 🎯 Success Metrics

### KPIs to Track:

1. **Conflict Rate**
   - Target: < 2% of all bookings
   - Current: Unknown (need to measure)

2. **Force Stop Rate**
   - Target: < 5% of all sessions
   - Monitor trend over time

3. **Average Idle Time Between Bookings**
   - Target: 5-10 minutes
   - Too high: Wasting capacity
   - Too low: More conflicts

4. **User Satisfaction**
   - Survey rating for booking experience
   - Target: > 4.0/5.0

5. **Revenue Impact**
   - Compare before/after buffer implementation
   - Monitor cancellation rate

---

## 🚨 Risks & Mitigation

### Risk 1: Giảm Availability

**Risk:** Buffer time giảm 15 phút mỗi slot → reduce capacity 25%

**Mitigation:**
- Monitor utilization rate
- Adjust buffer dynamically based on data
- Offer "fast turnaround" option với higher price

### Risk 2: User Frustration từ Force Stop

**Risk:** Users không thích bị force stop session

**Mitigation:**
- Clear communication trong app
- Warning 10 minutes trước
- Compensation: Free 10 minutes next time
- Option to "extend booking" by paying extra

### Risk 3: False Rejection

**Risk:** Reject valid booking vì ước tính sai

**Mitigation:**
- Conservative estimation (add safety margin)
- Allow staff override
- Collect feedback và improve algorithm

---

## 📚 References

### Related Files:
- `BookingService.java` - Booking creation logic
- `ChargingSessionService.java` - Session management
- `ChargingSimulatorService.java` - Charging simulation
- `ChargingPointStatusService.java` - Status management

### Related Docs:
- `booking-api.md` - Booking API documentation
- `charging-simulation-api.md` - Simulation API
- `CHARGING_SIMULATOR_SIMPLIFIED.md` - Simulator logic

### Database Tables:
- `bookings` - Booking records
- `charging_sessions` - Session records
- `charging_points` - Charging point status

---

## 📞 Next Steps

1. **Review & Approval:**
   - [ ] Technical lead review
   - [ ] Product owner approval
   - [ ] Stakeholder sign-off

2. **Implementation:**
   - [ ] Create JIRA tickets for Phase 1
   - [ ] Assign developers
   - [ ] Set up feature branch

3. **Testing:**
   - [ ] Write unit tests
   - [ ] Integration tests
   - [ ] Load testing
   - [ ] User acceptance testing

4. **Deployment:**
   - [ ] Deploy to staging
   - [ ] Monitor metrics
   - [ ] Gradual rollout to production
   - [ ] Post-deployment monitoring

---

**Document Version:** 1.0  
**Last Updated:** 22/11/2025  
**Author:** Technical Team  
**Status:** 📝 Draft - Pending Review

