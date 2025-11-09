package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Booking;
import com.swp.evchargingstation.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.chargingPoint.pointId = :chargingPointId AND b.bookingTime > :bookingTime ORDER BY b.bookingTime ASC")
    Optional<Booking> findFirstByChargingPointIdAndBookingTimeAfterOrderByBookingTimeAsc(@Param("chargingPointId") String chargingPointId, @Param("bookingTime") LocalDateTime bookingTime);

    List<Booking> findByBookingStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime time);

    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId AND b.chargingPoint.pointId = :chargingPointId AND b.bookingStatus = :status")
    Optional<Booking> findByUserIdAndChargingPointIdAndBookingStatus(@Param("userId") String userId, @Param("chargingPointId") String chargingPointId, @Param("status") BookingStatus status);
}

