package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.dto.BookingResponseDto;
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

        // Check if there's any conflicting booking at the requested time
        Optional<Booking> conflictingBooking = bookingRepository.findConflictingBooking(
                chargingPoint.getPointId(),
                bookingTime,
                bookingTime.plusHours(1) // Reserve at least 1 hour slot
        );

        if (conflictingBooking.isPresent()) {
            return BookingAvailabilityDto.builder()
                    .available(false)
                    .maxChargePercentage(0.0)
                    .message("Thời gian bạn chọn hiện không còn chỗ trống. Vui lòng chọn thời gian khác.")
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
            double availableEnergy = (chargingPoint.getChargingPower().getPowerKw() / 1000.0) * (timeSlot.toMinutes() / 60.0);
            maxChargePercentage = Math.min(100.0, (availableEnergy / vehicle.getBatteryCapacityKwh()) * 100);

            if (maxChargePercentage < 100.0) {
                message = String.format("Bạn có thể sạc tối đa đến %.1f%% (next booking starts at %s)",
                        maxChargePercentage, nextBooking.getBookingTime());
            }
        }

        return BookingAvailabilityDto.builder()
                .available(true)
                .maxChargePercentage(maxChargePercentage)
                .message(message)
                .build();
    }

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto, String email) {
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
                bookingRequestDto.getChargingPointId(),
                bookingRequestDto.getBookingTime(),
                bookingRequestDto.getVehicleId()
        );

        if (!availability.isAvailable()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        if (bookingRequestDto.getDesiredPercentage() > availability.getMaxChargePercentage()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        ChargingPoint chargingPoint = chargingPointRepository.findById(bookingRequestDto.getChargingPointId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        Vehicle vehicle = vehicleRepository.findById(bookingRequestDto.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Verify vehicle belongs to user (via Driver/owner)
        if (vehicle.getOwner() == null || !vehicle.getOwner().getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Calculate estimated end time
        double requiredEnergy = vehicle.getBatteryCapacityKwh() * (bookingRequestDto.getDesiredPercentage() / 100.0);
        double chargingTimeHours = requiredEnergy / (chargingPoint.getChargingPower().getPowerKw() / 1000.0);
        LocalDateTime estimatedEndTime = bookingRequestDto.getBookingTime().plusMinutes((long) (chargingTimeHours * 60));

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
        booking.setBookingTime(bookingRequestDto.getBookingTime());
        booking.setEstimatedEndTime(estimatedEndTime);
        booking.setDesiredPercentage(bookingRequestDto.getDesiredPercentage());
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
                savedBooking.getId(), user.getEmail(), chargingPoint.getName(), bookingRequestDto.getBookingTime());

        return convertToDto(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long bookingId, String email) {
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
    public List<BookingResponseDto> getUserBookings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId, String email) {
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

        // Refund deposit
        walletService.credit(user.getUserId(), DEPOSIT_AMOUNT, TransactionType.BOOKING_REFUND,
                String.format("Refund for cancelled booking #%d", bookingId),
                null, null, bookingId, null);

        log.info("Booking cancelled - ID: {}, User: {}", bookingId, email);

        return convertToDto(savedBooking);
    }

    @Transactional
    public BookingResponseDto checkInBooking(Long bookingId, String userId) {
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
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = booking.getBookingTime().minusMinutes(CHECK_IN_WINDOW_MINUTES);
        LocalDateTime checkInEnd = booking.getBookingTime().plusMinutes(CHECK_IN_WINDOW_MINUTES);

        // Validate check-in time window
        if (now.isBefore(checkInStart)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        if (now.isAfter(checkInEnd)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        // Update booking status to IN_PROGRESS
        // ChargingPoint status will be updated when charging session starts
        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking checked in - ID: {}, User: {}, Point: {}",
                bookingId, userId, booking.getChargingPoint().getName());

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

            // No refund for expired bookings - deposit is forfeited
            log.info("Booking expired - ID: {}, User: {}, Deposit forfeited",
                    booking.getId(), booking.getUser().getEmail());
        }

        if (!expiredBookings.isEmpty()) {
            log.info("Processed {} expired bookings", expiredBookings.size());
        }
    }

    private BookingResponseDto convertToDto(Booking booking) {
        return BookingResponseDto.builder()
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
                .build();
    }
}