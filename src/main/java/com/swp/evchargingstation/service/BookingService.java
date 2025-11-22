package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.request.BookingRequest;
import com.swp.evchargingstation.dto.response.BookingResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.BookingStatus;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingPointRepository chargingPointRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private static final double DEPOSIT_AMOUNT = 50000;
    private static final int CHECK_IN_WINDOW_MINUTES = 15; // Người dùng phải check-in trong vòng 15 phút
    private static final int BOOKING_EXPIRY_MINUTES = 15; // Booking tự động expire sau 15 phút nếu không check-in
    private static final int BUFFER_BETWEEN_BOOKINGS_MINUTES = 15; // Buffer time giữa các booking
    private static final int MIN_BOOKING_DURATION_MINUTES = 15; // Thời gian booking tối thiểu

    public BookingAvailabilityDto checkAvailability(String chargingPointId, LocalDateTime bookingTime, String vehicleId) {
        // Validate booking time (must be in future and within 24 hours)
        if (bookingTime.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        if (bookingTime.isAfter(LocalDateTime.now().plusHours(24))) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));

        // Check if charging point is operational
        if (chargingPoint.getStatus() == ChargingPointStatus.OUT_OF_SERVICE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // SOLUTION 1: Kiểm tra xem có active session đang chạy không
        ChargingSession activeSession = chargingPoint.getCurrentSession();
        if (activeSession != null && activeSession.getStatus() == com.swp.evchargingstation.enums.ChargingSessionStatus.IN_PROGRESS) {
            // Ước tính thời gian kết thúc session
            LocalDateTime estimatedEndTime = calculateEstimatedEndTime(activeSession);

            // Thêm buffer 15 phút để an toàn
            LocalDateTime safeAvailableTime = estimatedEndTime.plusMinutes(BUFFER_BETWEEN_BOOKINGS_MINUTES);

            if (safeAvailableTime.isAfter(bookingTime)) {
                return BookingAvailabilityDto.builder()
                        .available(false)
                        .maxChargePercentage(0.0)
                        .message(String.format("Trụ hiện đang có phiên sạc, dự kiến kết thúc lúc %02d:%02d. Thời gian sớm nhất có thể đặt: %02d:%02d",
                                estimatedEndTime.getHour(), estimatedEndTime.getMinute(),
                                safeAvailableTime.getHour(), safeAvailableTime.getMinute()))
                        .build();
            }
        }

        // SOLUTION 2: Kiểm tra buffer time với booking trước đó
        Optional<Booking> previousBooking = bookingRepository.findLastBookingBefore(
                chargingPoint.getPointId(), bookingTime);

        if (previousBooking.isPresent()) {
            LocalDateTime prevEndTime = previousBooking.get().getEstimatedEndTime();
            LocalDateTime minStartTime = prevEndTime.plusMinutes(BUFFER_BETWEEN_BOOKINGS_MINUTES);

            if (bookingTime.isBefore(minStartTime)) {
                return BookingAvailabilityDto.builder()
                        .available(false)
                        .maxChargePercentage(0.0)
                        .message(String.format("Cần buffer %d phút sau booking trước (kết thúc lúc %02d:%02d). Thời gian sớm nhất: %02d:%02d",
                                BUFFER_BETWEEN_BOOKINGS_MINUTES,
                                prevEndTime.getHour(), prevEndTime.getMinute(),
                                minStartTime.getHour(), minStartTime.getMinute()))
                        .build();
            }
        }

        // Tính estimated end time cho booking mới
        // Công suất thực tế = MIN(công suất trụ, công suất tối đa xe)
        double stationPowerKw = chargingPoint.getChargingPower().getPowerKw();
        double vehicleMaxPowerKw = vehicle.getMaxChargingPowerKw();
        double actualChargingPowerKw = Math.min(stationPowerKw, vehicleMaxPowerKw);

        // Năng lượng cần sạc từ current SOC -> 100%
        int currentSoc = vehicle.getCurrentSocPercent();
        double energyNeeded = ((100.0 - currentSoc) / 100.0) * vehicle.getBatteryCapacityKwh();

        // Thời gian sạc (giờ)
        double chargingTimeHours = energyNeeded / actualChargingPowerKw;

        // Giới hạn thời gian booking tối đa là 12 giờ (để tránh block trụ quá lâu)
        chargingTimeHours = Math.min(chargingTimeHours, 12.0);

        LocalDateTime maxEstimatedEndTime = bookingTime.plusMinutes((long) (chargingTimeHours * 60));

        // Check if there's any conflicting booking at the requested time
        Optional<Booking> conflictingBooking = bookingRepository.findConflictingBooking(
                chargingPoint.getPointId(),
                bookingTime,
                maxEstimatedEndTime
        );

        if (conflictingBooking.isPresent()) {
            Booking conflict = conflictingBooking.get();
            return BookingAvailabilityDto.builder()
                    .available(false)
                    .maxChargePercentage(0.0)
                    .message(String.format("Thời gian bạn chọn trùng với booking khác (%02d:%02d ngày %02d/%02d - %02d:%02d ngày %02d/%02d). Vui lòng chọn thời gian khác.",
                            conflict.getBookingTime().getHour(), conflict.getBookingTime().getMinute(),
                            conflict.getBookingTime().getDayOfMonth(), conflict.getBookingTime().getMonthValue(),
                            conflict.getEstimatedEndTime().getHour(), conflict.getEstimatedEndTime().getMinute(),
                            conflict.getEstimatedEndTime().getDayOfMonth(), conflict.getEstimatedEndTime().getMonthValue()))
                    .build();
        }

        // Find next booking to calculate max charge percentage
        Optional<Booking> nextBookingOpt = bookingRepository.findFirstByChargingPointPointIdAndBookingTimeAfterOrderByBookingTimeAsc(
                chargingPoint.getPointId(), bookingTime);

        double maxChargePercentage = 100.0;
        String message = "Bạn có thể sạc tối đa đến 100%.";

        if (nextBookingOpt.isPresent()) {
            Booking nextBooking = nextBookingOpt.get();
            Duration timeSlot = Duration.between(bookingTime, nextBooking.getBookingTime());

            // SOLUTION 2: Trừ đi buffer time
            long availableMinutes = timeSlot.toMinutes() - BUFFER_BETWEEN_BOOKINGS_MINUTES;

            if (availableMinutes < MIN_BOOKING_DURATION_MINUTES) {
                return BookingAvailabilityDto.builder()
                        .available(false)
                        .maxChargePercentage(0.0)
                        .message(String.format("Không đủ thời gian giữa các booking (cần tối thiểu %d phút + %d phút buffer)",
                                MIN_BOOKING_DURATION_MINUTES, BUFFER_BETWEEN_BOOKINGS_MINUTES))
                        .build();
            }

            // Tính công suất thực tế = MIN(trụ, xe)
            double actualPowerKw = Math.min(
                chargingPoint.getChargingPower().getPowerKw(),
                vehicle.getMaxChargingPowerKw()
            );

            // Năng lượng có thể sạc trong thời gian available
            double availableEnergy = actualPowerKw * (availableMinutes / 60.0);

            // % pin có thể sạc (từ current SOC đã khai báo ở trên)
            double maxSocIncrease = (availableEnergy / vehicle.getBatteryCapacityKwh()) * 100;
            maxChargePercentage = Math.min(100.0, currentSoc + maxSocIncrease);

            if (maxChargePercentage < 100.0) {
                message = String.format("Bạn có tối đa %d phút sạc (đến %.1f%%). Booking tiếp theo: %02d:%02d",
                        availableMinutes, maxChargePercentage,
                        nextBooking.getBookingTime().getHour(), nextBooking.getBookingTime().getMinute());
            }
        }

        return BookingAvailabilityDto.builder()
                .available(true)
                .maxChargePercentage(maxChargePercentage)
                .message(message)
                .build();
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user has any active booking
        Optional<Booking> activeBooking = bookingRepository.findActiveBookingByUser(user.getUserId());
        if (activeBooking.isPresent()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Check wallet balance
        double currentBalance = walletService.getBalance(user.getUserId());
        if (currentBalance < DEPOSIT_AMOUNT) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        // Check availability
        BookingAvailabilityDto availability = checkAvailability(
                bookingRequest.getChargingPointId(),
                bookingRequest.getBookingTime(),
                bookingRequest.getVehicleId()
        );

        if (!availability.isAvailable()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        if (bookingRequest.getDesiredPercentage() > availability.getMaxChargePercentage()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        ChargingPoint chargingPoint = chargingPointRepository.findById(bookingRequest.getChargingPointId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        Vehicle vehicle = vehicleRepository.findById(bookingRequest.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Verify vehicle belongs to user (via Driver/owner)
        if (vehicle.getOwner() == null || !vehicle.getOwner().getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Calculate estimated end time
        // Công suất thực tế = MIN(trụ, xe)
        double actualPowerKw = Math.min(
            chargingPoint.getChargingPower().getPowerKw(),
            vehicle.getMaxChargingPowerKw()
        );

        // Năng lượng cần sạc từ current SOC -> desired SOC
        int currentSoc = vehicle.getCurrentSocPercent();
        double socIncrease = bookingRequest.getDesiredPercentage() - currentSoc;
        double requiredEnergy = (socIncrease / 100.0) * vehicle.getBatteryCapacityKwh();

        // Thời gian sạc
        double chargingTimeHours = requiredEnergy / actualPowerKw;
        LocalDateTime estimatedEndTime = bookingRequest.getBookingTime().plusMinutes((long) (chargingTimeHours * 60));

        // Debit deposit from wallet
        walletService.debit(user.getUserId(), DEPOSIT_AMOUNT, TransactionType.BOOKING_DEPOSIT,
                String.format("Booking deposit for %s at %s",
                        chargingPoint.getName(), chargingPoint.getStation().getName()),
                null, null);

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setChargingPoint(chargingPoint);
        booking.setBookingTime(bookingRequest.getBookingTime());
        booking.setEstimatedEndTime(estimatedEndTime);
        booking.setDesiredPercentage(bookingRequest.getDesiredPercentage());
        booking.setDepositAmount(DEPOSIT_AMOUNT);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        // QUAN TRỌNG: KHÔNG set chargingPoint.status = RESERVED ngay tại đây
        // Lý do:
        // - Một trụ có thể có nhiều booking trong ngày (A lúc 8:00, B lúc 10:00)
        // - Nếu set RESERVED ngay khi A book (giả sử lúc 7:00 sáng) thì trụ sẽ "chết"
        //   từ 7:00 -> 8:00 mặc dù đang trống
        // - Giải pháp: Trạng thái RESERVED được tính động dựa trên booking sắp tới
        //   (trong vòng 15-30 phút) thông qua ChargingPointStatusService
        // - Scheduled job sẽ tự động cập nhật trạng thái vật lý khi cần thiết

        log.info("Booking created successfully - ID: {}, User: {}, Point: {}, Time: {}",
                savedBooking.getId(), user.getEmail(), chargingPoint.getName(), bookingRequest.getBookingTime());

        return convertToDto(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Verify booking belongs to user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        return convertToDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Verify booking belongs to user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Only allow cancellation of CONFIRMED bookings
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Check if booking time has passed
        if (booking.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Update booking status
        booking.setBookingStatus(BookingStatus.CANCELLED_BY_USER);
        Booking savedBooking = bookingRepository.save(booking);

        // KHÔNG HOÀN TIỀN CỌC - User mất cọc khi hủy booking
        // Tiền cọc sẽ bị tịch thu làm phí phạt
        log.info("Booking cancelled by user - ID: {}, User: {} - Deposit FORFEITED (no refund)",
                bookingId, email);

        return convertToDto(savedBooking);
    }

    @Transactional
    public BookingResponse checkInBooking(Long bookingId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Verify booking belongs to user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Only allow check-in for CONFIRMED bookings
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            log.warn("Check-in rejected - Booking #{} status is {}, expected CONFIRMED",
                     bookingId, booking.getBookingStatus());
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = booking.getBookingTime().minusMinutes(CHECK_IN_WINDOW_MINUTES);
        LocalDateTime checkInEnd = booking.getBookingTime().plusMinutes(CHECK_IN_WINDOW_MINUTES);

        // Validate check-in time window
        if (now.isBefore(checkInStart)) {
            log.warn("Check-in too early - Booking #{} at {}, current time: {}, can check-in from: {}",
                     bookingId, booking.getBookingTime(), now, checkInStart);
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        if (now.isAfter(checkInEnd)) {
            log.warn("Check-in too late - Booking #{} expired. Booking time: {}, current time: {}, deadline was: {}",
                     bookingId, booking.getBookingTime(), now, checkInEnd);
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Update booking status to IN_PROGRESS
        // ChargingPoint status will be updated when charging session starts
        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        booking.setCheckedInAt(LocalDateTime.now()); // Track check-in time
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking checked in - ID: {}, User: {}, Point: {}, Time: {}",
                bookingId, userId, booking.getChargingPoint().getName(), LocalDateTime.now());

        return convertToDto(savedBooking);
    }

    @Scheduled(cron = "0 */5 * * * *") // Run every 5 minutes
    @Transactional
    public void processExpiredBookings() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(BOOKING_EXPIRY_MINUTES);

        // Find bookings that are CONFIRMED but past their check-in window
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.CONFIRMED,
                expiryThreshold
        );

        for (Booking booking : expiredBookings) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // FIX BUG #3: Free up the charging point
            ChargingPoint point = booking.getChargingPoint();
            if (point.getStatus() == ChargingPointStatus.RESERVED && point.getCurrentSession() == null) {
                point.setStatus(ChargingPointStatus.AVAILABLE);
                chargingPointRepository.save(point);
                log.info("Freed up charging point {} after booking #{} expired",
                         point.getName(), booking.getId());
            }

            // No refund for expired bookings - deposit is forfeited
            log.info("Booking expired - ID: {}, User: {}, Deposit forfeited",
                    booking.getId(), booking.getUser().getEmail());
        }

        if (!expiredBookings.isEmpty()) {
            log.info("Processed {} expired bookings", expiredBookings.size());
        }
    }

    /**
     * NEW: Auto-cancel bookings đã check-in nhưng không start session trong 10 phút
     * Chạy mỗi 2 phút để check timeout
     */
    @Scheduled(cron = "0 */2 * * * *") // Run every 2 minutes
    @Transactional
    public void processCheckedInTimeouts() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);

        // Tìm bookings đã check-in (IN_PROGRESS) nhưng chưa có session
        List<Booking> checkedInBookings = bookingRepository.findByBookingStatus(BookingStatus.IN_PROGRESS);

        int timeoutCount = 0;
        for (Booking booking : checkedInBookings) {
            // Skip nếu chưa có checkedInAt (dữ liệu cũ)
            if (booking.getCheckedInAt() == null) {
                continue;
            }

            // Check timeout: check-in > 10 phút mà chưa có session
            if (booking.getCheckedInAt().isBefore(timeoutThreshold)) {
                // Kiểm tra xem đã có session chưa bằng cách check ChargingPoint
                ChargingPoint point = booking.getChargingPoint();
                boolean hasActiveSession = point.getCurrentSession() != null &&
                                          point.getCurrentSession().getStatus() == com.swp.evchargingstation.enums.ChargingSessionStatus.IN_PROGRESS;

                if (!hasActiveSession) {
                    // Timeout: Check-in rồi nhưng không start session
                    booking.setBookingStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);

                    // Free up charging point
                    if (point.getStatus() == ChargingPointStatus.RESERVED && point.getCurrentSession() == null) {
                        point.setStatus(ChargingPointStatus.AVAILABLE);
                        chargingPointRepository.save(point);
                    }

                    // Refund 50% deposit (penalty for not starting)
                    double refundAmount = booking.getDepositAmount() * 0.5;
                    walletService.credit(
                        booking.getUser().getUserId(),
                        refundAmount,
                        TransactionType.BOOKING_REFUND,
                        String.format("Partial refund (50%%) for booking #%d - check-in timeout", booking.getId()),
                        null, null, booking.getId(), null
                    );

                    log.warn("⚠️ Booking #{} check-in timeout - User: {}, Point: {}, Refunded: {} VND (50%)",
                            booking.getId(),
                            booking.getUser().getEmail(),
                            point.getName(),
                            refundAmount);

                    timeoutCount++;
                }
            }
        }

        if (timeoutCount > 0) {
            log.info("Processed {} check-in timeouts (10 minutes)", timeoutCount);
        }
    }

    private BookingResponse convertToDto(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userName(booking.getUser().getFullName())
                .userEmail(booking.getUser().getEmail())
                .vehicleModel(booking.getVehicle().getModel().name())
                .vehicleLicensePlate(booking.getVehicle().getLicensePlate())
                .chargingPointName(booking.getChargingPoint().getName())
                .stationName(booking.getChargingPoint().getStation().getName())
                .stationAddress(booking.getChargingPoint().getStation().getAddress())
                .bookingTime(booking.getBookingTime())
                .estimatedEndTime(booking.getEstimatedEndTime())
                .desiredPercentage(booking.getDesiredPercentage())
                .depositAmount(booking.getDepositAmount())
                .bookingStatus(booking.getBookingStatus())
                .createdAt(booking.getCreatedAt())
                // Fields để frontend auto-start session
                .chargingPointId(booking.getChargingPoint().getPointId())
                .vehicleId(booking.getVehicle().getVehicleId())
                .currentSocPercent(booking.getVehicle().getCurrentSocPercent())
                .build();
    }

    /**
     * Tính toán thời gian dự kiến kết thúc session dựa trên:
     * - SOC hiện tại và target SOC
     * - Charging power của trụ
     * - Battery capacity của xe
     * - Safety margin 20% (charging curve, nhiệt độ, etc.)
     */
    private LocalDateTime calculateEstimatedEndTime(ChargingSession session) {
        double currentSoc = session.getStartSocPercent(); // SOC hiện tại
        double targetSoc = session.getTargetSocPercent() != null ? session.getTargetSocPercent() : 100;
        double remainingPercent = targetSoc - currentSoc;

        if (remainingPercent <= 0) {
            // Nếu đã đạt target hoặc target không hợp lệ, assume sẽ kết thúc trong 5 phút
            return LocalDateTime.now().plusMinutes(5);
        }

        Vehicle vehicle = session.getVehicle();
        ChargingPoint point = session.getChargingPoint();

        // Công suất thực tế = MIN(trụ, xe)
        double actualPowerKw = Math.min(
            point.getChargingPower().getPowerKw(),
            vehicle.getMaxChargingPowerKw()
        );

        double requiredEnergy = (remainingPercent / 100.0) * vehicle.getBatteryCapacityKwh();

        // Tính thời gian lý thuyết
        double hoursNeeded = requiredEnergy / actualPowerKw;

        // Thêm 20% safety margin để tính đến charging curve (sạc chậm dần khi gần đầy)
        double safetyFactor = 1.2;
        double adjustedHours = hoursNeeded * safetyFactor;

        long minutesNeeded = (long) (adjustedHours * 60);

        return LocalDateTime.now().plusMinutes(minutesNeeded);
    }
}