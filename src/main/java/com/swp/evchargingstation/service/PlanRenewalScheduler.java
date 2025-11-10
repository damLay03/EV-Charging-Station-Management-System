package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Plan;
import com.swp.evchargingstation.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanRenewalScheduler {

    private final DriverRepository driverRepository;
    private final PlanService planService;

    /**
     * Chạy hàng ngày lúc 00:00 để kiểm tra và gia hạn plan cho driver
     * Chỉ gia hạn plan "Cao cấp" và "Tiết kiệm" khi đến ngày đăng ký của tháng sau
     */
    @Scheduled(cron = "0 0 0 * * ?") // Chạy lúc 00:00 mỗi ngày
    public void autoRenewPlans() {
        log.info("Starting automatic plan renewal job at {}", LocalDateTime.now());

        try {
            // Lấy tất cả driver có plan
            List<Driver> driversWithPlan = driverRepository.findAll().stream()
                    .filter(driver -> driver.getPlan() != null)
                    .filter(driver -> driver.getPlanSubscriptionDate() != null)
                    .toList();

            log.info("Found {} drivers with active plans", driversWithPlan.size());

            int renewedCount = 0;
            int failedCount = 0;

            for (Driver driver : driversWithPlan) {
                Plan plan = driver.getPlan();

                // Chỉ xử lý plan "Cao cấp" và "Tiết kiệm"
                if (!plan.getName().equalsIgnoreCase("Cao cấp") &&
                    !plan.getName().equalsIgnoreCase("Tiết kiệm")) {
                    continue;
                }

                // Kiểm tra xem đã đến ngày gia hạn chưa
                LocalDateTime subscriptionDate = driver.getPlanSubscriptionDate();
                LocalDateTime now = LocalDateTime.now();

                // Tính ngày gia hạn tiếp theo (30 ngày sau ngày đăng ký)
                LocalDateTime nextRenewalDate = subscriptionDate.plusDays(30);

                // Chỉ gia hạn nếu HÔM NAY là ngày gia hạn hoặc đã quá ngày
                // So sánh theo ngày (bỏ qua giờ phút giây)
                if (nextRenewalDate.toLocalDate().isBefore(now.toLocalDate()) ||
                    nextRenewalDate.toLocalDate().isEqual(now.toLocalDate())) {

                    log.info("Attempting to renew plan {} for driver {}. Next renewal date: {}",
                            plan.getName(), driver.getUserId(), nextRenewalDate.toLocalDate());

                    boolean success = planService.autoRenewPlan(driver);
                    if (success) {
                        renewedCount++;
                    } else {
                        failedCount++;
                    }
                }
            }

            log.info("Completed automatic plan renewal job. Renewed: {}, Failed: {}",
                    renewedCount, failedCount);

        } catch (Exception e) {
            log.error("Error during automatic plan renewal job: {}", e.getMessage(), e);
        }
    }

    /**
     * Test method - chạy mỗi 5 phút để test (comment out trong production)
     * Uncomment dòng dưới để test
     */
    // @Scheduled(cron = "0 */5 * * * ?") // Chạy mỗi 5 phút
    public void autoRenewPlansTest() {
        log.info("[TEST] Running plan renewal check at {}", LocalDateTime.now());
        autoRenewPlans();
    }
}

