package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.Booking;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.repository.BookingRepository;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý trạng thái động của ChargingPoint dựa trên Booking.
 *
 * Nguyên tắc hoạt động:
 * - ChargingPoint.status chỉ phản ánh trạng thái vật lý thực tế (AVAILABLE, CHARGING, OUT_OF_SERVICE, MAINTENANCE).
 * - Trạng thái RESERVED được tính toán động dựa trên booking sắp tới (trong vòng 15-30 phút).
 * - Khi hiển thị cho client, sử dụng displayStatus (có thể là RESERVED nếu có booking sắp tới).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargingPointStatusService {

    private final BookingRepository bookingRepository;
    private final ChargingPointRepository chargingPointRepository;

    // Khoảng thời gian trước giờ booking để hiển thị trạng thái RESERVED
    private static final int RESERVE_WINDOW_MINUTES = 30; // 30 phút trước giờ booking

    // Khoảng thời gian sau giờ booking để vẫn giữ RESERVED (cho phép check-in muộn)
    private static final int RESERVE_GRACE_MINUTES = 10; // 10 phút sau giờ booking

    /**
     * Tính toán trạng thái hiển thị động cho một charging point.
     *
     * Logic:
     * - Nếu trụ đang OUT_OF_SERVICE hoặc MAINTENANCE -> giữ nguyên
     * - Nếu trụ đang CHARGING -> giữ nguyên
     * - Nếu trụ AVAILABLE và có booking trong khoảng [now, now + 30 phút] -> trả về RESERVED
     * - Còn lại -> trả về trạng thái hiện tại
     *
     * @param chargingPointId ID của charging point
     * @return Trạng thái hiển thị (có thể là RESERVED nếu có booking sắp tới)
     */
    public ChargingPointStatus calculateDisplayStatus(String chargingPointId) {
        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElse(null);

        if (chargingPoint == null) {
            return ChargingPointStatus.OUT_OF_SERVICE;
        }

        // Nếu trụ đang có vấn đề hoặc đang sạc, giữ nguyên trạng thái
        if (chargingPoint.getStatus() == ChargingPointStatus.OUT_OF_SERVICE ||
            chargingPoint.getStatus() == ChargingPointStatus.MAINTENANCE ||
            chargingPoint.getStatus() == ChargingPointStatus.CHARGING ||
            chargingPoint.getStatus() == ChargingPointStatus.OCCUPIED) {
            return chargingPoint.getStatus();
        }

        // Kiểm tra có booking sắp tới không
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(RESERVE_GRACE_MINUTES); // Cho phép check-in muộn
        LocalDateTime windowEnd = now.plusMinutes(RESERVE_WINDOW_MINUTES);

        Optional<Booking> upcomingBooking = bookingRepository.findUpcomingBookingInTimeWindow(
                chargingPointId, windowStart, windowEnd);

        if (upcomingBooking.isPresent()) {
            // Có booking sắp tới -> hiển thị RESERVED
            return ChargingPointStatus.RESERVED;
        }

        // Không có booking -> trả về trạng thái thực tế
        return chargingPoint.getStatus();
    }

    /**
     * Lấy thông tin booking sắp tới cho một charging point (nếu có).
     *
     * @param chargingPointId ID của charging point
     * @return Optional booking sắp tới trong khoảng reserve window
     */
    public Optional<Booking> getUpcomingBooking(String chargingPointId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(RESERVE_GRACE_MINUTES);
        LocalDateTime windowEnd = now.plusMinutes(RESERVE_WINDOW_MINUTES);

        return bookingRepository.findUpcomingBookingInTimeWindow(
                chargingPointId, windowStart, windowEnd);
    }

    /**
     * Scheduled job: Tự động cập nhật trạng thái RESERVED cho các trụ có booking sắp tới.
     * Chạy mỗi 5 phút.
     *
     * QUAN TRỌNG: Đây là cách tiếp cận dự phòng. Trong thực tế, nên sử dụng displayStatus động
     * thay vì thay đổi trạng thái vật lý của trụ.
     *
     * Job này CHỈ cập nhật trạng thái vật lý khi:
     * - Booking trong vòng 15 phút tới
     * - Trụ hiện đang AVAILABLE
     * - Chưa có session đang chạy
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    @Transactional
    public void updateReservedStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now;
        LocalDateTime windowEnd = now.plusMinutes(15); // Chỉ set RESERVED trong vòng 15 phút

        List<Booking> nearbyBookings = bookingRepository.findBookingsNearStartTime(windowStart, windowEnd);

        int updatedCount = 0;
        for (Booking booking : nearbyBookings) {
            ChargingPoint point = booking.getChargingPoint();

            // Chỉ cập nhật nếu trụ đang AVAILABLE và không có session
            if (point.getStatus() == ChargingPointStatus.AVAILABLE &&
                point.getCurrentSession() == null) {

                point.setStatus(ChargingPointStatus.RESERVED);
                chargingPointRepository.save(point);
                updatedCount++;

                log.info("Set ChargingPoint {} to RESERVED for booking #{} (booking time: {})",
                        point.getName(), booking.getId(), booking.getBookingTime());
            }
        }

        if (updatedCount > 0) {
            log.info("Updated {} charging points to RESERVED status", updatedCount);
        }
    }

    /**
     * Scheduled job: Tự động release trạng thái RESERVED về AVAILABLE sau khi booking expired.
     * Chạy mỗi 5 phút.
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    @Transactional
    public void releaseExpiredReservations() {
        // Tìm tất cả trụ đang RESERVED
        List<ChargingPoint> reservedPoints = chargingPointRepository.findAll().stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.RESERVED)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        int releasedCount = 0;

        for (ChargingPoint point : reservedPoints) {
            // Kiểm tra xem còn booking active trong khoảng reserve window không
            Optional<Booking> upcomingBooking = getUpcomingBooking(point.getPointId());

            if (upcomingBooking.isEmpty()) {
                // Không còn booking sắp tới -> release về AVAILABLE
                point.setStatus(ChargingPointStatus.AVAILABLE);
                chargingPointRepository.save(point);
                releasedCount++;

                log.info("Released ChargingPoint {} from RESERVED to AVAILABLE (no upcoming booking)",
                        point.getName());
            }
        }

        if (releasedCount > 0) {
            log.info("Released {} charging points from RESERVED status", releasedCount);
        }
    }

    /**
     * Kiểm tra xem một charging point có thể booking được không trong khoảng thời gian nhất định.
     *
     * Logic:
     * - Kiểm tra trạng thái vật lý (không cho book nếu OUT_OF_SERVICE, MAINTENANCE)
     * - Kiểm tra có booking chồng lấn thời gian không
     *
     * @param chargingPointId ID của charging point
     * @param startTime Thời gian bắt đầu
     * @param endTime Thời gian kết thúc
     * @return true nếu có thể booking, false nếu không
     */
    public boolean isAvailableForBooking(String chargingPointId, LocalDateTime startTime, LocalDateTime endTime) {
        ChargingPoint point = chargingPointRepository.findById(chargingPointId)
                .orElse(null);

        if (point == null) {
            return false;
        }

        // Không cho book nếu trụ đang bảo trì hoặc hỏng
        if (point.getStatus() == ChargingPointStatus.OUT_OF_SERVICE ||
            point.getStatus() == ChargingPointStatus.MAINTENANCE) {
            return false;
        }

        // Kiểm tra có booking chồng lấn không
        Optional<Booking> conflicting = bookingRepository.findConflictingBooking(
                chargingPointId, startTime, endTime);

        return conflicting.isEmpty();
    }
}

