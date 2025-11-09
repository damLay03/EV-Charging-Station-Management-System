package com.swp.evchargingstation.service.impl;

import com.swp.evchargingstation.dto.BookingAvailabilityDto;
import com.swp.evchargingstation.dto.BookingRequestDto;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.BookingStatus;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.TransactionType;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.*;
import com.swp.evchargingstation.service.BookingService;
import com.swp.evchargingstation.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingPointRepository chargingPointRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private static final double DEPOSIT_AMOUNT = 50000;

    @Override
    public BookingAvailabilityDto checkAvailability(String chargingPointId, LocalDateTime bookingTime, String vehicleId) {
        if (bookingTime.isBefore(LocalDateTime.now()) || bookingTime.isAfter(LocalDateTime.now().plusHours(24))) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        Optional<Booking> nextBookingOpt = bookingRepository.findFirstByChargingPointIdAndBookingTimeAfterOrderByBookingTimeAsc(
                chargingPoint.getPointId(), bookingTime);

        double maxChargePercentage = 100.0;
        String message = "You can charge to 100%.";

        if (nextBookingOpt.isPresent()) {
            Booking nextBooking = nextBookingOpt.get();
            Duration timeSlot = Duration.between(bookingTime, nextBooking.getBookingTime());
            double availableEnergy = (chargingPoint.getChargingPower().getPowerKw() / 1000.0) * (timeSlot.toMinutes() / 60.0);
            maxChargePercentage = (availableEnergy / vehicle.getBatteryCapacityKwh()) * 100;
            message = "You can only charge up to " + String.format("%.2f", maxChargePercentage) +
                    "% (session will end at " + nextBooking.getBookingTime() + " for the next user).";
        }

        return BookingAvailabilityDto.builder()
                .available(true)
                .maxChargePercentage(maxChargePercentage)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public Booking createBooking(BookingRequestDto bookingRequestDto, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check wallet balance
        if (walletService.getBalance(userId) < DEPOSIT_AMOUNT) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        BookingAvailabilityDto availability = checkAvailability(bookingRequestDto.getChargingPointId(),
                bookingRequestDto.getBookingTime(), bookingRequestDto.getVehicleId());

        if (bookingRequestDto.getDesiredPercentage() > availability.getMaxChargePercentage()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }

        ChargingPoint chargingPoint = chargingPointRepository.findById(bookingRequestDto.getChargingPointId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        Vehicle vehicle = vehicleRepository.findById(bookingRequestDto.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Calculate estimated end time
        double requiredEnergy = vehicle.getBatteryCapacityKwh() * (bookingRequestDto.getDesiredPercentage() / 100.0);
        double chargingTimeHours = requiredEnergy / (chargingPoint.getChargingPower().getPowerKw() / 1000.0);
        LocalDateTime estimatedEndTime = bookingRequestDto.getBookingTime().plusMinutes((long) (chargingTimeHours * 60));

        // Debit deposit from wallet
        walletService.debit(userId, DEPOSIT_AMOUNT, TransactionType.BOOKING_DEPOSIT,
                "Booking deposit for charging point " + chargingPoint.getPointId(), null, null);


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

        chargingPoint.setStatus(ChargingPointStatus.RESERVED);
        chargingPointRepository.save(chargingPoint);

        return bookingRepository.save(booking);
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processExpiredBookings() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<Booking> expiredBookings = bookingRepository.findByBookingStatusAndBookingTimeBefore(
                BookingStatus.CONFIRMED, tenMinutesAgo);

        for (Booking booking : expiredBookings) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            ChargingPoint chargingPoint = booking.getChargingPoint();
            chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
            chargingPointRepository.save(chargingPoint);
        }
    }
}
