package com.swp.evchargingstation.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class để bật tính năng Scheduling cho Spring Boot.
 *
 * @EnableScheduling annotation cho phép Spring tự động:
 * - Quét và phát hiện tất cả các method có annotation @Scheduled trong project
 * - Thực thi các scheduled jobs theo cron expression đã định nghĩa
 * - Quản lý thread pool cho các task chạy định kỳ
 *
 * Các Scheduled Jobs đang hoạt động trong hệ thống:
 *
 * 1. BookingService:
 *    - processExpiredBookings(): Chạy mỗi 5 phút (0 *\/5 * * * *)
 *      → Tự động hủy các booking quá hạn không check-in
 *
 * 2. ChargingPointStatusService:
 *    - updateReservedStatus(): Chạy mỗi 5 phút (0 *\/5 * * * *)
 *      → Set trạng thái RESERVED cho trụ có booking trong vòng 15 phút
 *    - releaseExpiredReservations(): Chạy mỗi 5 phút (0 *\/5 * * * *)
 *      → Release trạng thái RESERVED về AVAILABLE sau khi booking hết hạn
 *
 * 3. ChargingSimulatorService (nếu có):
 *    - Các job mô phỏng quá trình sạc pin
 *
 * QUAN TRỌNG: Nếu xóa @EnableScheduling, tất cả scheduled jobs sẽ KHÔNG chạy!
 *
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see com.swp.evchargingstation.service.BookingService#processExpiredBookings()
 * @see com.swp.evchargingstation.service.ChargingPointStatusService#updateReservedStatus()
 * @see com.swp.evchargingstation.service.ChargingPointStatusService#releaseExpiredReservations()
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Class này chỉ cần annotations để hoạt động.
    // Không cần thêm method hay bean nào khác.
    //
    // Để thêm scheduled job mới:
    // 1. Tạo method trong bất kỳ @Service nào
    // 2. Thêm @Scheduled(cron = "...") lên method đó
    // 3. Spring sẽ tự động detect và chạy
}

