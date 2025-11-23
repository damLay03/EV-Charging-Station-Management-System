# 🚀 Spring Application Events - Implementation Guide

**Project:** EV Charging Station Management System  
**Status:** Ready to Implement  
**Effort:** ~2-3 weeks

---

## 📋 TABLE OF CONTENTS

1. [Cấu trúc thư mục](#cấu-trúc-thư-mục)
2. [Configuration](#configuration)
3. [Phase 1: ChargingSession Events](#phase-1-chargingsession-events)
4. [Phase 2: Booking Events](#phase-2-booking-events)
5. [Phase 3: Wallet Events](#phase-3-wallet-events)
6. [Testing Strategy](#testing-strategy)
7. [Monitoring & Troubleshooting](#monitoring--troubleshooting)

---

## 📁 CẤU TRÚC THƯ MỤC

```
src/main/java/com/swp/evchargingstation/
├── event/                          # ← NEW
│   ├── session/
│   │   ├── ChargingSessionStartedEvent.java
│   │   ├── ChargingSessionCompletedEvent.java
│   │   └── ChargingSessionCancelledEvent.java
│   ├── booking/
│   │   ├── BookingCreatedEvent.java
│   │   ├── BookingCheckedInEvent.java
│   │   ├── BookingCancelledEvent.java
│   │   └── BookingExpiredEvent.java
│   ├── wallet/
│   │   ├── WalletCreditedEvent.java
│   │   └── WalletDebitedEvent.java
│   └── payment/
│       ├── PaymentCompletedEvent.java
│       └── PaymentFailedEvent.java
├── listener/                       # ← NEW
│   ├── ChargingSessionEventListener.java
│   ├── BookingEventListener.java
│   ├── WalletEventListener.java
│   └── PaymentEventListener.java
├── config/
│   └── AsyncEventConfig.java       # ← NEW
└── service/
    ├── ChargingSimulatorService.java  # ← REFACTOR
    ├── BookingService.java            # ← REFACTOR
    ├── WalletService.java             # ← REFACTOR
    └── ...
```

---

## ⚙️ CONFIGURATION

### 1. AsyncEventConfig.java

```java
package com.swp.evchargingstation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncEventConfig implements AsyncConfigurer {

    /**
     * Thread pool cho async event listeners
     */
    @Bean(name = "eventExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Handle exceptions trong async listeners
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Async event listener failed - Method: {}, Params: {}, Error: {}",
                        method.getName(), params, ex.getMessage(), ex);
                
                // TODO: Send alert to monitoring system
                // TODO: Store failed event in dead letter queue
            }
        };
    }
}
```

### 2. application.yaml (optional tuning)

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 10
        queue-capacity: 100
      thread-name-prefix: event-
```

---

## 🎯 PHASE 1: CHARGINGSESSION EVENTS

### Priority: **HIGH** (Tuần 1-2)

### 1.1. Event Classes

#### ChargingSessionStartedEvent.java
```java
package com.swp.evchargingstation.event.session;

import com.swp.evchargingstation.entity.ChargingSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi charging session bắt đầu.
 * 
 * Listeners:
 * - EmailNotificationListener: Gửi email thông báo
 * - AnalyticsListener: Track analytics
 */
@Getter
public class ChargingSessionStartedEvent extends ApplicationEvent {
    private final ChargingSession session;
    private final String driverId;
    private final String vehicleId;
    
    public ChargingSessionStartedEvent(Object source, ChargingSession session) {
        super(source);
        this.session = session;
        this.driverId = session.getDriver().getUserId();
        this.vehicleId = session.getVehicle().getVehicleId();
    }
}
```

#### ChargingSessionCompletedEvent.java
```java
package com.swp.evchargingstation.event.session;

import com.swp.evchargingstation.entity.ChargingSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi charging session hoàn tất.
 * 
 * Listeners:
 * - PaymentSettlementListener: Xử lý thanh toán (SYNC, REQUIRES_NEW)
 * - EmailNotificationListener: Gửi email hoàn tất (ASYNC)
 * - ChargingPointStatusListener: Update trạng thái trụ
 * - BookingCompletionListener: Mark booking as completed
 */
@Getter
public class ChargingSessionCompletedEvent extends ApplicationEvent {
    private final ChargingSession session;
    private final String sessionId;
    private final float totalCost;
    private final float energyKwh;
    
    public ChargingSessionCompletedEvent(Object source, ChargingSession session) {
        super(source);
        this.session = session;
        this.sessionId = session.getSessionId();
        this.totalCost = session.getCostTotal();
        this.energyKwh = session.getEnergyKwh();
    }
}
```

---

### 1.2. Refactor ChargingSimulatorService

#### BEFORE:
```java
@Transactional
public void completeSession(String sessionId) {
    // Core logic
    session.setStatus(COMPLETED);
    chargingSessionRepository.save(session);
    
    // ❌ Side effects
    emailService.sendChargingCompleteEmail(session);
    paymentSettlementService.settlePaymentForCompletedSession(session, cost);
}
```

#### AFTER:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingSimulatorService {
    
    private final ChargingSessionRepository chargingSessionRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingPointRepository chargingPointRepository;
    // ✅ REMOVED: EmailService, PaymentSettlementService
    
    private final ApplicationEventPublisher eventPublisher;  // ← NEW
    
    /**
     * Complete charging session.
     * Publishes ChargingSessionCompletedEvent for side effects.
     */
    @Transactional
    public void completeSession(String sessionId) {
        try {
            ChargingSession session = chargingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

            if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
                log.warn("Session {} is not IN_PROGRESS, status: {}", sessionId, session.getStatus());
                return;
            }

            Vehicle vehicle = session.getVehicle();
            ChargingPoint point = session.getChargingPoint();

            // ===== CORE BUSINESS LOGIC =====
            session.setStatus(ChargingSessionStatus.COMPLETED);
            session.setEndTime(LocalDateTime.now());
            
            // Save all changes
            chargingSessionRepository.save(session);
            vehicleRepository.save(vehicle);
            chargingPointRepository.save(point);

            log.info("[CompleteSession] Session {} completed. Cost: {}, Energy: {} kWh",
                    sessionId, session.getCostTotal(), session.getEnergyKwh());

            // ===== PUBLISH EVENT FOR SIDE EFFECTS =====
            // Transaction commits here ↓
            eventPublisher.publishEvent(
                new ChargingSessionCompletedEvent(this, session)
            );
            
            log.info("[CompleteSession] Published ChargingSessionCompletedEvent for session {}", sessionId);

        } catch (Exception ex) {
            log.error("[CompleteSession] Failed to complete session {}: {}", sessionId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

---

### 1.3. Event Listener

#### ChargingSessionEventListener.java
```java
package com.swp.evchargingstation.listener;

import com.swp.evchargingstation.event.session.ChargingSessionCompletedEvent;
import com.swp.evchargingstation.event.session.ChargingSessionStartedEvent;
import com.swp.evchargingstation.service.EmailService;
import com.swp.evchargingstation.service.PaymentSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChargingSessionEventListener {

    private final EmailService emailService;
    private final PaymentSettlementService paymentSettlementService;

    /**
     * Send email notification when session starts (ASYNC)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendStartNotification(ChargingSessionStartedEvent event) {
        try {
            log.info("[Event] Sending start email for session: {}", event.getSession().getSessionId());
            emailService.sendChargingStartEmail(event.getSession());
            log.info("[Event] Start email sent successfully for session: {}", event.getSession().getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send start email for session {}: {}",
                    event.getSession().getSessionId(), ex.getMessage(), ex);
            // Don't rethrow - email failure should not affect main flow
        }
    }

    /**
     * Send email notification when session completes (ASYNC)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendCompletionNotification(ChargingSessionCompletedEvent event) {
        try {
            log.info("[Event] Sending completion email for session: {}", event.getSessionId());
            emailService.sendChargingCompleteEmail(event.getSession());
            log.info("[Event] Completion email sent successfully for session: {}", event.getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send completion email for session {}: {}",
                    event.getSessionId(), ex.getMessage(), ex);
        }
    }

    /**
     * Settle payment when session completes (SYNC, SEPARATE TRANSACTION)
     * 
     * ⚠️ IMPORTANT:
     * - This MUST be synchronous to ensure payment is processed
     * - Uses REQUIRES_NEW to have separate transaction
     * - If payment fails, session is still COMPLETED (no rollback)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settlePayment(ChargingSessionCompletedEvent event) {
        try {
            log.info("[Event] Settling payment for session: {}", event.getSessionId());
            paymentSettlementService.settlePaymentForCompletedSession(
                    event.getSession(),
                    event.getTotalCost()
            );
            log.info("[Event] Payment settled successfully for session: {}", event.getSessionId());
        } catch (Exception ex) {
            log.error("[Event] Failed to settle payment for session {}: {}",
                    event.getSessionId(), ex.getMessage(), ex);
            // Payment failure is logged but doesn't affect session completion
            // User will see UNPAID status in their dashboard
        }
    }
}
```

---

### 1.4. Testing

#### ChargingSimulatorServiceTest.java
```java
@SpringBootTest
class ChargingSimulatorServiceTest {

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ChargingSimulatorService chargingSimulatorService;

    @Test
    void completeSession_shouldPublishEvent() {
        // Arrange
        String sessionId = "test-session-id";
        // ... setup session ...

        // Act
        chargingSimulatorService.completeSession(sessionId);

        // Assert
        verify(eventPublisher, times(1))
                .publishEvent(any(ChargingSessionCompletedEvent.class));
    }
}
```

#### ChargingSessionEventListenerTest.java
```java
@SpringBootTest
class ChargingSessionEventListenerTest {

    @MockBean
    private EmailService emailService;

    @MockBean
    private PaymentSettlementService paymentSettlementService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void whenSessionCompleted_shouldSendEmail() throws InterruptedException {
        // Arrange
        ChargingSession session = createTestSession();
        ChargingSessionCompletedEvent event = new ChargingSessionCompletedEvent(this, session);

        // Act
        eventPublisher.publishEvent(event);

        // Wait for async processing
        Thread.sleep(1000);

        // Assert
        verify(emailService, times(1)).sendChargingCompleteEmail(session);
        verify(paymentSettlementService, times(1))
                .settlePaymentForCompletedSession(eq(session), anyFloat());
    }
}
```

---

## 🎯 PHASE 2: BOOKING EVENTS

### Priority: **HIGH** (Tuần 2-3)

### 2.1. Event Classes

#### BookingCreatedEvent.java
```java
package com.swp.evchargingstation.event.booking;

import com.swp.evchargingstation.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi booking được tạo.
 * 
 * Listeners:
 * - WalletDepositListener: Debit deposit từ wallet (SYNC, REQUIRES_NEW)
 * - EmailNotificationListener: Gửi email xác nhận (ASYNC)
 * - ChargingPointReservationListener: Update reservation status
 */
@Getter
public class BookingCreatedEvent extends ApplicationEvent {
    private final Booking booking;
    private final Long bookingId;
    private final String userId;
    private final double depositAmount;
    
    public BookingCreatedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
        this.bookingId = booking.getId();
        this.userId = booking.getUser().getUserId();
        this.depositAmount = booking.getDepositAmount();
    }
}
```

#### BookingCheckedInEvent.java
```java
package com.swp.evchargingstation.event.booking;

import com.swp.evchargingstation.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookingCheckedInEvent extends ApplicationEvent {
    private final Booking booking;
    private final Long bookingId;
    
    public BookingCheckedInEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
        this.bookingId = booking.getId();
    }
}
```

---

### 2.2. Refactor BookingService

#### BEFORE:
```java
@Transactional
public BookingResponse createBooking(BookingRequest request, String email) {
    // ... validation ...
    
    // ❌ Direct wallet call
    walletService.debit(user.getUserId(), DEPOSIT_AMOUNT, ...);
    
    Booking booking = bookingRepository.save(booking);
    return convertToDto(booking);
}
```

#### AFTER:
```java
@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    // ✅ REMOVED: WalletService dependency
    
    private final ApplicationEventPublisher eventPublisher;  // ← NEW
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String email) {
        // ... validation (checkAvailability, etc.) ...
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // ===== CORE BUSINESS LOGIC =====
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setChargingPoint(chargingPoint);
        booking.setBookingTime(request.getBookingTime());
        booking.setDepositAmount(DEPOSIT_AMOUNT);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created - ID: {}, User: {}, Point: {}", 
                savedBooking.getId(), email, chargingPoint.getName());

        // ===== PUBLISH EVENT FOR SIDE EFFECTS =====
        eventPublisher.publishEvent(
            new BookingCreatedEvent(this, savedBooking)
        );

        return convertToDto(savedBooking);
    }
}
```

---

### 2.3. Event Listener

#### BookingEventListener.java
```java
package com.swp.evchargingstation.listener;

import com.swp.evchargingstation.event.booking.BookingCreatedEvent;
import com.swp.evchargingstation.service.EmailService;
import com.swp.evchargingstation.service.WalletService;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final WalletService walletService;
    private final EmailService emailService;

    /**
     * Debit deposit from wallet when booking is created (SYNC, SEPARATE TRANSACTION)
     * 
     * ⚠️ CRITICAL: This must succeed before booking is confirmed to user
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void debitDeposit(BookingCreatedEvent event) {
        try {
            log.info("[Event] Debiting deposit for booking: {}", event.getBookingId());
            
            walletService.debit(
                    event.getUserId(),
                    event.getDepositAmount(),
                    TransactionType.BOOKING_DEPOSIT,
                    String.format("Booking deposit for booking #%d", event.getBookingId()),
                    event.getBookingId(),
                    null
            );
            
            log.info("[Event] Deposit debited successfully for booking: {}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to debit deposit for booking {}: {}",
                    event.getBookingId(), ex.getMessage(), ex);
            // TODO: Mark booking as PAYMENT_FAILED or auto-cancel
            throw ex; // Rethrow to trigger rollback if needed
        }
    }

    /**
     * Send booking confirmation email (ASYNC)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventExecutor")
    public void sendConfirmationEmail(BookingCreatedEvent event) {
        try {
            log.info("[Event] Sending confirmation email for booking: {}", event.getBookingId());
            emailService.sendBookingConfirmationEmail(event.getBooking());
            log.info("[Event] Confirmation email sent for booking: {}", event.getBookingId());
        } catch (Exception ex) {
            log.error("[Event] Failed to send confirmation email for booking {}: {}",
                    event.getBookingId(), ex.getMessage(), ex);
        }
    }
}
```

---

## 🎯 PHASE 3: WALLET EVENTS

### Priority: **MEDIUM** (Tuần 3-4)

### 3.1. Event Classes

#### WalletCreditedEvent.java
```java
package com.swp.evchargingstation.event.wallet;

import com.swp.evchargingstation.entity.Wallet;
import com.swp.evchargingstation.enums.TransactionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WalletCreditedEvent extends ApplicationEvent {
    private final Wallet wallet;
    private final String userId;
    private final Double amount;
    private final TransactionType transactionType;
    private final Double newBalance;
    
    public WalletCreditedEvent(Object source, Wallet wallet, Double amount, TransactionType type) {
        super(source);
        this.wallet = wallet;
        this.userId = wallet.getUser().getUserId();
        this.amount = amount;
        this.transactionType = type;
        this.newBalance = wallet.getBalance();
    }
    
    public boolean isTopUp() {
        return transactionType == TransactionType.TOPUP_CASH || 
               transactionType == TransactionType.TOPUP_ZALOPAY;
    }
}
```

---

### 3.2. Refactor WalletService

#### BEFORE:
```java
@Transactional
public WalletTransaction credit(String userId, Double amount, ...) {
    wallet.setBalance(balance + amount);
    walletRepository.save(wallet);
    
    // ❌ Email logic trong transaction
    if (type == TransactionType.TOPUP_CASH || type == TransactionType.TOPUP_ZALOPAY) {
        emailService.sendWalletTopUpSuccessEmail(user, amount, balance);
    }
    
    return transaction;
}
```

#### AFTER:
```java
@Service
@RequiredArgsConstructor
public class WalletService {
    
    private final WalletRepository walletRepository;
    // ✅ REMOVED: EmailService dependency
    
    private final ApplicationEventPublisher eventPublisher;  // ← NEW
    
    @Transactional
    public WalletTransaction credit(String userId, Double amount, TransactionType type, ...) {
        Wallet wallet = getWallet(userId);
        
        // ===== CORE BUSINESS LOGIC =====
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .status(TransactionStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .description(description)
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        log.info("Credited {} to wallet of user {}. New balance: {}", 
                amount, userId, wallet.getBalance());

        // ===== PUBLISH EVENT =====
        eventPublisher.publishEvent(
            new WalletCreditedEvent(this, wallet, amount, type)
        );

        return transaction;
    }
}
```

---

## 🧪 TESTING STRATEGY

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @InjectMocks
    private BookingService bookingService;
    
    @Test
    void createBooking_shouldPublishBookingCreatedEvent() {
        // Arrange
        BookingRequest request = createTestRequest();
        
        // Act
        bookingService.createBooking(request, "test@example.com");
        
        // Assert
        ArgumentCaptor<BookingCreatedEvent> eventCaptor = 
            ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        BookingCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getBookingId()).isNotNull();
        assertThat(event.getDepositAmount()).isEqualTo(50000);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class BookingEventIntegrationTest {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private WalletService walletService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Test
    void createBooking_shouldDebitDepositFromWallet() throws InterruptedException {
        // Arrange
        String userId = "test-user";
        walletService.credit(userId, 100000, ...); // Top up first
        
        // Act
        BookingResponse booking = bookingService.createBooking(request, email);
        
        // Wait for event processing
        Thread.sleep(1000);
        
        // Assert
        Double balance = walletService.getBalance(userId);
        assertThat(balance).isEqualTo(50000); // 100000 - 50000 deposit
    }
}
```

---

## 📊 MONITORING & TROUBLESHOOTING

### Logging Best Practices

```java
@Component
@Slf4j
public class EventLoggingAspect {
    
    @Around("@annotation(org.springframework.transaction.event.TransactionalEventListener)")
    public Object logEventListener(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("[EventListener] START - Method: {}, Args: {}", methodName, args);
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("[EventListener] SUCCESS - Method: {}, Duration: {}ms", methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("[EventListener] FAILED - Method: {}, Duration: {}ms, Error: {}",
                    methodName, duration, ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

### Dead Letter Queue (Optional)

```java
@Component
@Slf4j
public class EventFailureHandler {
    
    private final Queue<FailedEvent> deadLetterQueue = new LinkedList<>();
    
    public void handleFailedEvent(ApplicationEvent event, Exception ex) {
        FailedEvent failedEvent = FailedEvent.builder()
                .event(event)
                .exception(ex)
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        deadLetterQueue.offer(failedEvent);
        log.error("Event failed and added to DLQ: {}", event.getClass().getSimpleName());
    }
    
    @Scheduled(fixedDelay = 60000) // Retry every minute
    public void retryFailedEvents() {
        while (!deadLetterQueue.isEmpty()) {
            FailedEvent failedEvent = deadLetterQueue.poll();
            // Retry logic...
        }
    }
}
```

---

## ✅ CHECKLIST

### Phase 1: ChargingSession Events
- [ ] Create event classes
- [ ] Create event listener
- [ ] Refactor ChargingSimulatorService
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Deploy to staging
- [ ] Monitor for 1 week
- [ ] Deploy to production

### Phase 2: Booking Events
- [ ] Create event classes
- [ ] Create event listener
- [ ] Refactor BookingService
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Deploy to staging
- [ ] Monitor for 1 week
- [ ] Deploy to production

### Phase 3: Wallet Events
- [ ] Create event classes
- [ ] Create event listener
- [ ] Refactor WalletService
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Deploy to staging
- [ ] Monitor for 1 week
- [ ] Deploy to production

---

## 📚 REFERENCES

- [Spring Application Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Transaction Event Listeners](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- [Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

---

**Last Updated:** 23/11/2025  
**Author:** GitHub Copilot  
**Status:** Ready for Implementation

