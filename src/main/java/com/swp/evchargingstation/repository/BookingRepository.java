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

    @Query("SELECT b FROM Booking b WHERE b.chargingPoint.pointId = :chargingPointId " +
           "AND b.bookingTime > :bookingTime " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime ASC")
    Optional<Booking> findFirstByChargingPointPointIdAndBookingTimeAfterOrderByBookingTimeAsc(
            @Param("chargingPointId") String chargingPointId,
            @Param("bookingTime") LocalDateTime bookingTime);

    @Query("SELECT b FROM Booking b WHERE b.chargingPoint.pointId = :chargingPointId " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND ((b.bookingTime BETWEEN :startTime AND :endTime) " +
           "OR (b.estimatedEndTime BETWEEN :startTime AND :endTime))")
    Optional<Booking> findConflictingBooking(
            @Param("chargingPointId") String chargingPointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime DESC")
    Optional<Booking> findActiveBookingByUser(@Param("userId") String userId);

    List<Booking> findByUserUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = :status " +
           "AND b.bookingTime < :expiryTime")
    List<Booking> findExpiredBookings(
            @Param("status") BookingStatus status,
            @Param("expiryTime") LocalDateTime expiryTime);

    List<Booking> findByBookingStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime time);

    @Query("SELECT b FROM Booking b WHERE b.user.userId = :userId " +
           "AND b.chargingPoint.pointId = :chargingPointId " +
           "AND b.bookingStatus = :status")
    Optional<Booking> findByUserIdAndChargingPointIdAndBookingStatus(
            @Param("userId") String userId,
            @Param("chargingPointId") String chargingPointId,
            @Param("status") BookingStatus status);

    /**
     * Tìm booking sắp tới cho một charging point trong khoảng thời gian
     * (để hiển thị trạng thái RESERVED động)
     */
    @Query("SELECT b FROM Booking b WHERE b.chargingPoint.pointId = :chargingPointId " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND b.bookingTime BETWEEN :startTime AND :endTime " +
           "ORDER BY b.bookingTime ASC")
    Optional<Booking> findUpcomingBookingInTimeWindow(
            @Param("chargingPointId") String chargingPointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Tìm tất cả booking cần được đặt thành RESERVED (gần đến giờ booking)
     */
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = 'CONFIRMED' " +
           "AND b.bookingTime BETWEEN :startTime AND :endTime")
    List<Booking> findBookingsNearStartTime(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Tìm booking cuối cùng trước một thời điểm (để kiểm tra buffer time)
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime < :time " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime DESC")
    Optional<Booking> findLastBookingBefore(
            @Param("pointId") String pointId,
            @Param("time") LocalDateTime time);

    /**
     * Tìm booking tiếp theo sau một thời điểm (để kiểm tra buffer time)
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime > :time " +
           "AND b.bookingStatus IN ('CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY b.bookingTime ASC")
    Optional<Booking> findNextBookingAfter(
            @Param("pointId") String pointId,
            @Param("time") LocalDateTime time);

    /**
     * Tìm các booking sắp tới cho một trụ (để auto-terminate session)
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.chargingPoint.pointId = :pointId " +
           "AND b.bookingTime BETWEEN :start AND :end " +
           "AND b.bookingStatus = 'CONFIRMED' " +
           "ORDER BY b.bookingTime ASC")
    List<Booking> findUpcomingBookingsForPoint(
            @Param("pointId") String pointId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Tìm tất cả bookings theo status (để check timeout)
     */
    List<Booking> findByBookingStatus(BookingStatus status);
}

