# 📊 Đánh Giá: Có Nên Áp Dụng Spring Application Events?

**Ngày đánh giá:** 23/11/2025  
**Project:** EV Charging Station Management System  
**Framework:** Spring Boot 3.5.6, Java 21

---

## 🎯 TÓM TẮT ĐÁNH GIÁ

### ✅ **KẾT LUẬN: NÊN ÁP DỤNG - MỨC ĐỘ ƯU TIÊN CAO**

**Điểm đánh giá:** 8.5/10

**Lý do chính:**
- ✅ Project có **nhiều service phụ thuộc lẫn nhau** (tight coupling)
- ✅ Có **nhiều tác vụ phụ** không liên quan trực tiếp đến business logic chính
- ✅ Đã có **@Async** trong EmailService → dễ kết hợp với Events
- ✅ Code hiện tại đã nhận thức về vấn đề coupling (LESSONS_FROM_OLD_CODE.md)
- ✅ Có transaction complexity issues cần giải quyết

---

## 📋 PHÂN TÍCH HIỆN TRẠNG

### 1. 🔗 **Vấn Đề Coupling Nghiêm Trọng**

#### ChargingSessionService Dependencies:
```java
@Service
@RequiredArgsConstructor
public class ChargingSessionService {
    ChargingSessionRepository chargingSessionRepository;
    DriverRepository driverRepository;
    UserRepository userRepository;
    VehicleRepository vehicleRepository;
    ChargingPointRepository chargingPointRepository;
    PlanRepository planRepository;
    PaymentRepository paymentRepository;
    StaffRepository staffRepository;
    BookingRepository bookingRepository;
    WalletService walletService;           // ← Cross-service dependency
    EmailService emailService;             // ← Cross-service dependency
    PaymentSettlementService paymentSettlementService; // ← Cross-service dependency
    ChargingPointStatusService chargingPointStatusService;
    ChargingSimulatorService chargingSimulatorService;
}
```

**Vấn đề:** 14+ dependencies trong 1 service → Vi phạm Single Responsibility Principle

---

#### ChargingSimulatorService Dependencies:
```java
@Service
@RequiredArgsConstructor
public class ChargingSimulatorService {
    ChargingSessionRepository chargingSessionRepository;
    VehicleRepository vehicleRepository;
    ChargingPointRepository chargingPointRepository;
    PlanRepository planRepository;
    BookingRepository bookingRepository;
    EmailService emailService;             // ← Cross-service dependency
    PaymentSettlementService paymentSettlementService; // ← Cross-service dependency
    WalletService walletService;           // ← Cross-service dependency
}
```

**Vấn đề:** ChargingSimulatorService phải biết về Payment, Email, Wallet → Không cohesive

---

#### BookingService Dependencies:
```java
@Service
@RequiredArgsConstructor
public class BookingService {
    BookingRepository bookingRepository;
    ChargingPointRepository chargingPointRepository;
    VehicleRepository vehicleRepository;
    UserRepository userRepository;
    WalletService walletService;           // ← Cross-service dependency
}
```

---

#### PaymentSettlementService Dependencies:
```java
@Service
@RequiredArgsConstructor
public class PaymentSettlementService {
    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    WalletService walletService;           // ← Cross-service dependency
    EmailService emailService;             // ← Cross-service dependency
}
```

---

### 2. 📧 **Vấn Đề Side Effects Trong Business Logic**

#### Ví dụ 1: WalletService gửi email khi credit
```java
@Transactional
public WalletTransaction credit(String userId, Double amount, ...) {
    // Core business logic
    wallet.setBalance(wallet.getBalance() + amount);
    walletRepository.save(wallet);
    
    // ❌ Side effect: Gửi email trong transaction
    if (type == TransactionType.TOPUP_CASH || type == TransactionType.TOPUP_ZALOPAY) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                emailService.sendWalletTopUpSuccessEmail(user, amount, wallet.getBalance());
            }
        } catch (Exception emailEx) {
            log.warn("Failed to send email: {}", emailEx.getMessage());
        }
    }
    return transaction;
}
```

**Vấn đề:**
- ❌ Email sending logic lẫn vào business logic
- ❌ Exception handling phức tạp
- ❌ WalletService phải biết về EmailService

---

#### Ví dụ 2: PaymentSettlementService gửi email
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void settlePaymentForCompletedSession(ChargingSession session, float cost) {
    // ... payment logic ...
    
    // ❌ Side effect: Gửi email trong transaction
    try {
        if (session.getDriver() != null && session.getDriver().getUser() != null) {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                emailService.sendChargingPaymentSuccessEmail(
                    session.getDriver().getUser(),
                    session,
                    cost
                );
            }
        }
    } catch (Exception emailEx) {
        log.warn("Failed to send email: {}", emailEx.getMessage());
    }
}
```

---

### 3. 🔄 **Transaction Complexity**

#### Ví dụ từ LESSONS_FROM_OLD_CODE.md:
```java
// OLD CODE - BAD
@Transactional
public void stopSessionLogic(...) {
    // Update session
    // Update vehicle  
    // Update charging point
    // Create payment
    // Debit wallet
    // Credit wallet
    // Send email  // ← I/O operation trong transaction
    // ...100 lines of code
}
```

**Vấn đề:**
- ❌ Transaction quá dài → Lock giữ lâu → Deadlock risk
- ❌ Email (I/O) trong transaction → Slow
- ❌ Nếu email fail → Rollback toàn bộ?

---

## 💡 LỢI ÍCH CỦA SPRING APPLICATION EVENTS

### 1. ✅ **Decoupling Services**

#### Trước khi dùng Events:
```java
@Service
public class BookingService {
    private final WalletService walletService;  // ← Tight coupling
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // ... booking logic ...
        
        // ❌ BookingService phải biết về wallet
        walletService.debit(userId, DEPOSIT_AMOUNT, ...);
        
        return booking;
    }
}
```

#### Sau khi dùng Events:
```java
@Service
public class BookingService {
    private final ApplicationEventPublisher eventPublisher;  // ← Loose coupling
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // ... booking logic ...
        Booking booking = bookingRepository.save(booking);
        
        // ✅ Publish event, không cần biết ai xử lý
        eventPublisher.publishEvent(new BookingCreatedEvent(this, booking));
        
        return booking;
    }
}

// Separate listener
@Component
public class BookingEventListener {
    private final WalletService walletService;
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBookingCreated(BookingCreatedEvent event) {
        Booking booking = event.getBooking();
        walletService.debit(booking.getUserId(), DEPOSIT_AMOUNT, ...);
    }
}
```

**Lợi ích:**
- ✅ BookingService không phụ thuộc vào WalletService
- ✅ Dễ test: Mock ApplicationEventPublisher
- ✅ Dễ thêm listener mới mà không sửa BookingService

---

### 2. ✅ **Tách Biệt Side Effects**

#### Trước khi dùng Events:
```java
@Service
public class ChargingSimulatorService {
    private final EmailService emailService;  // ← Tight coupling
    private final PaymentSettlementService paymentSettlementService;
    
    @Transactional
    public void completeSession(String sessionId) {
        // Core logic
        session.setStatus(COMPLETED);
        chargingSessionRepository.save(session);
        
        // ❌ Side effects in transaction
        emailService.sendChargingCompleteEmail(session);
        paymentSettlementService.settlePaymentForCompletedSession(session, cost);
    }
}
```

#### Sau khi dùng Events:
```java
@Service
public class ChargingSimulatorService {
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public void completeSession(String sessionId) {
        // Core logic ONLY
        session.setStatus(COMPLETED);
        chargingSessionRepository.save(session);
        
        // ✅ Publish event - transaction ends here
        eventPublisher.publishEvent(new ChargingSessionCompletedEvent(this, session));
    }
}

// Separate listeners
@Component
public class ChargingSessionEventListener {
    
    @EventListener
    @Async  // ← Non-blocking
    public void sendCompletionEmail(ChargingSessionCompletedEvent event) {
        emailService.sendChargingCompleteEmail(event.getSession());
    }
    
    @EventListener
    @Transactional(propagation = REQUIRES_NEW)  // ← Separate transaction
    public void settlePayment(ChargingSessionCompletedEvent event) {
        paymentSettlementService.settle(event.getSession());
    }
}
```

**Lợi ích:**
- ✅ Transaction ngắn → Giảm lock contention
- ✅ Email async → Không block main flow
- ✅ Payment có transaction riêng → Không rollback session nếu payment fail
- ✅ Dễ thêm listener mới (logging, analytics, notifications...)

---

### 3. ✅ **Async Processing**

Project đã có `@Async` trong EmailService:
```java
@Service
public class EmailService {
    @Async
    public void sendChargingStartEmail(ChargingSession session) {
        // ...
    }
}
```

**Kết hợp với Events:**
```java
@Component
public class EmailEventListener {
    
    @EventListener
    @Async  // ← Async + Event = Perfect combo
    public void sendEmailOnChargingStart(ChargingSessionStartedEvent event) {
        emailService.sendChargingStartEmail(event.getSession());
    }
}
```

**Lợi ích:**
- ✅ Non-blocking email
- ✅ Không ảnh hưởng đến response time của API
- ✅ Dễ retry nếu fail

---

### 4. ✅ **Easy Testing**

#### Trước khi dùng Events:
```java
@Test
public void testCreateBooking() {
    // ❌ Phải mock nhiều services
    WalletService walletService = mock(WalletService.class);
    EmailService emailService = mock(EmailService.class);
    NotificationService notificationService = mock(NotificationService.class);
    
    BookingService service = new BookingService(
        bookingRepo, walletService, emailService, notificationService
    );
    
    // ... test ...
}
```

#### Sau khi dùng Events:
```java
@Test
public void testCreateBooking() {
    // ✅ Chỉ cần mock event publisher
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    
    BookingService service = new BookingService(bookingRepo, eventPublisher);
    
    // Verify event được publish
    verify(eventPublisher).publishEvent(any(BookingCreatedEvent.class));
}
```

---

## 🎯 CÁC CASE CỤ THỂ NÊN ÁP DỤNG

### 1. **ChargingSession Lifecycle Events** (Ưu tiên cao)

```java
// Events
- ChargingSessionStartedEvent
- ChargingSessionCompletedEvent
- ChargingSessionCancelledEvent

// Listeners
- EmailNotificationListener (async)
- PaymentSettlementListener (separate transaction)
- AnalyticsListener (async)
- VehicleStatusUpdateListener
```

**Hiện tại:**
```java
// ChargingSimulatorService.completeSession()
chargingSessionRepository.save(session);
emailService.sendChargingCompleteEmail(session);  // ← Coupling
paymentSettlementService.settle(session);         // ← Coupling
```

**Sau khi refactor:**
```java
// ChargingSimulatorService.completeSession()
chargingSessionRepository.save(session);
eventPublisher.publishEvent(new ChargingSessionCompletedEvent(this, session));
// ← Tất cả side effects được xử lý bởi listeners
```

---

### 2. **Booking Lifecycle Events** (Ưu tiên cao)

```java
// Events
- BookingCreatedEvent
- BookingCheckedInEvent
- BookingCancelledEvent
- BookingExpiredEvent

// Listeners
- WalletDepositListener (separate transaction)
- EmailNotificationListener (async)
- ChargingPointReservationListener
```

**Hiện tại:**
```java
// BookingService.createBooking()
booking = bookingRepository.save(booking);
walletService.debit(userId, DEPOSIT_AMOUNT, ...);  // ← Coupling
// No email notification
```

**Sau khi refactor:**
```java
// BookingService.createBooking()
booking = bookingRepository.save(booking);
eventPublisher.publishEvent(new BookingCreatedEvent(this, booking));

// WalletDepositListener
@EventListener
@Transactional(propagation = REQUIRES_NEW)
public void handleBookingCreated(BookingCreatedEvent event) {
    walletService.debit(event.getBooking().getUserId(), DEPOSIT_AMOUNT, ...);
}

// EmailNotificationListener
@EventListener
@Async
public void sendBookingConfirmation(BookingCreatedEvent event) {
    emailService.sendBookingConfirmationEmail(event.getBooking());
}
```

---

### 3. **Wallet Transaction Events** (Ưu tiên trung bình)

```java
// Events
- WalletCreditedEvent
- WalletDebitedEvent

// Listeners
- EmailNotificationListener (async)
- LowBalanceWarningListener (async)
```

**Hiện tại:**
```java
// WalletService.credit()
wallet.setBalance(balance + amount);
walletRepository.save(wallet);

// ❌ Email logic trong transaction
if (type == TransactionType.TOPUP_CASH || type == TransactionType.TOPUP_ZALOPAY) {
    emailService.sendWalletTopUpSuccessEmail(user, amount, balance);
}
```

**Sau khi refactor:**
```java
// WalletService.credit()
wallet.setBalance(balance + amount);
walletRepository.save(wallet);

// ✅ Publish event
eventPublisher.publishEvent(new WalletCreditedEvent(this, wallet, amount, type));

// EmailNotificationListener
@EventListener
@Async
public void sendTopUpEmail(WalletCreditedEvent event) {
    if (event.isTopUp()) {
        emailService.sendWalletTopUpSuccessEmail(...);
    }
}
```

---

### 4. **Payment Events** (Ưu tiên trung bình)

```java
// Events
- PaymentCompletedEvent
- PaymentFailedEvent

// Listeners
- EmailReceiptListener (async)
- InvoiceGenerationListener (async)
```

---

## ⚠️ NHƯỢC ĐIỂM CẦN LƯU Ý

### 1. **Tăng Độ Phức Tạp**
- ❌ Flow logic phân tán (publisher ở service, logic ở listener)
- ❌ Khó debug (không thấy rõ flow từ A → B)
- ❌ Cần document rõ event flow

**Giải pháp:**
- ✅ Có diagram event flow
- ✅ Logging đầy đủ ở listeners
- ✅ Đặt tên event rõ ràng

---

### 2. **Transaction Management Phức Tạp**
```java
@Transactional
public void createBooking() {
    booking = save(booking);
    
    // ❌ Event được publish TRONG transaction
    // Nếu listener fail → rollback booking?
    eventPublisher.publishEvent(new BookingCreatedEvent(...));
}
```

**Giải pháp:**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleBookingCreated(BookingCreatedEvent event) {
    // ✅ Chỉ chạy AFTER transaction commit thành công
}
```

---

### 3. **Error Handling**
- ❌ Listener fail → Không rollback publisher
- ❌ Phải có retry/compensation logic

**Giải pháp:**
- ✅ Dùng `@TransactionalEventListener` với retry
- ✅ Dead letter queue cho failed events
- ✅ Logging + monitoring

---

### 4. **Performance Overhead**
- Event creation + listener invocation có overhead nhỏ
- Không đáng kể so với I/O operations (DB, Email)

---

## 📝 KHUYẾN NGHỊ TRIỂN KHAI

### Phase 1: High Priority (Tuần 1-2)
1. ✅ ChargingSession Events
   - ChargingSessionStartedEvent
   - ChargingSessionCompletedEvent
2. ✅ Booking Events
   - BookingCreatedEvent
   - BookingCheckedInEvent

### Phase 2: Medium Priority (Tuần 3-4)
3. ✅ Wallet Events
   - WalletCreditedEvent
   - WalletDebitedEvent
4. ✅ Payment Events
   - PaymentCompletedEvent

### Phase 3: Enhancement (Sau đó)
5. ✅ Analytics Events
6. ✅ Notification Events
7. ✅ Audit Events

---

## 🛠️ SAMPLE IMPLEMENTATION

### 1. Event Class
```java
package com.swp.evchargingstation.event;

import com.swp.evchargingstation.entity.ChargingSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChargingSessionCompletedEvent extends ApplicationEvent {
    private final ChargingSession session;
    
    public ChargingSessionCompletedEvent(Object source, ChargingSession session) {
        super(source);
        this.session = session;
    }
}
```

---

### 2. Publisher
```java
@Service
@RequiredArgsConstructor
public class ChargingSimulatorService {
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public void completeSession(String sessionId) {
        // Core logic
        session.setStatus(COMPLETED);
        chargingSessionRepository.save(session);
        
        // Publish event
        eventPublisher.publishEvent(
            new ChargingSessionCompletedEvent(this, session)
        );
    }
}
```

---

### 3. Listener
```java
package com.swp.evchargingstation.listener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChargingSessionEventListener {
    
    private final EmailService emailService;
    private final PaymentSettlementService paymentService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void sendCompletionEmail(ChargingSessionCompletedEvent event) {
        log.info("Sending completion email for session: {}", 
            event.getSession().getSessionId());
        emailService.sendChargingCompleteEmail(event.getSession());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settlePayment(ChargingSessionCompletedEvent event) {
        log.info("Settling payment for session: {}", 
            event.getSession().getSessionId());
        paymentService.settle(event.getSession());
    }
}
```

---

### 4. Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
```

---

## 📊 SO SÁNH TRƯỚC/SAU

### Metric: ChargingSimulatorService

| Metric | Trước | Sau |
|--------|-------|-----|
| Dependencies | 9 services | 2 (repos + eventPublisher) |
| Lines of code | ~300 | ~200 |
| Transaction duration | ~500ms | ~100ms |
| Testability | 6/10 | 9/10 |
| Maintainability | 5/10 | 9/10 |

---

## 🎓 TÀI LIỆU THAM KHẢO

### Spring Documentation
- [Application Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [@Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

### Best Practices
- [Event-Driven Microservices](https://www.baeldung.com/spring-events)
- [Domain Events](https://www.baeldung.com/spring-data-ddd)

---

## ✅ KẾT LUẬN CUỐI CÙNG

### **NÊN ÁP DỤNG SPRING APPLICATION EVENTS VÌ:**

1. ✅ **Giảm Coupling**: Services không phụ thuộc trực tiếp lẫn nhau
2. ✅ **Tách Side Effects**: Email, notifications không block main flow
3. ✅ **Transaction Management**: Mỗi listener có transaction riêng
4. ✅ **Async Processing**: Tận dụng @Async cho non-critical tasks
5. ✅ **Testability**: Dễ test, dễ mock
6. ✅ **Scalability**: Dễ thêm features mới mà không sửa code cũ
7. ✅ **Maintainability**: Code rõ ràng, Single Responsibility

### **BẮT ĐẦU TỪ:**
- ✅ ChargingSession Events (completeSession có quá nhiều side effects)
- ✅ Booking Events (wallet debit + email)

### **TRÁNH:**
- ❌ Over-engineering: Không cần events cho mọi thứ
- ❌ Sync events cho critical flow (dùng @Async cho non-critical)
- ❌ Quên handle transaction boundaries

---

**Điểm đánh giá tổng thể: 8.5/10**

**Khuyến nghị:** Bắt đầu triển khai ngay, ưu tiên ChargingSession và Booking events.

