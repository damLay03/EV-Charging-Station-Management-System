package com.swp.evchargingstation.configuration;

import com.swp.evchargingstation.entity.Admin;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.repository.AdminRepository;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.StaffRepository;
import com.swp.evchargingstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleSpecificBackfillRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final StaffRepository staffRepository;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long createdDrivers = 0, createdStaffs = 0, createdAdmins = 0;
        for (User user : userRepository.findAll()) {
            Role role = user.getRole();
            if (role == null) continue;
            switch (role) {
                case DRIVER:
                    if (driverRepository.findById(user.getUserId()).isEmpty()) {
                        Driver driver = Driver.builder()
                                .user(user)
                                .address(null)
                                .joinDate(LocalDateTime.now())
                                .build();
                        driverRepository.save(driver);
                        createdDrivers++;
                        log.info("Backfilled DRIVER record for user {}", user.getUserId());
                    }
                    break;
                case STAFF:
                    if (staffRepository.findById(user.getUserId()).isEmpty()) {
                        Staff staff = Staff.builder()
                                .user(user)
                                .employeeNo(null)
                                .position(null)
                                .build();
                        staffRepository.save(staff);
                        createdStaffs++;
                        log.info("Backfilled STAFF record for user {}", user.getUserId());
                    }
                    break;
                case ADMIN:
                    if (adminRepository.findById(user.getUserId()).isEmpty()) {
                        Admin admin = Admin.builder()
                                .user(user)
                                .build();
                        adminRepository.save(admin);
                        createdAdmins++;
                        log.info("Backfilled ADMIN record for user {}", user.getUserId());
                    }
                    break;
            }
        }
        log.info("Role-specific backfill done. Created: drivers={}, staffs={}, admins={}", createdDrivers, createdStaffs, createdAdmins);
    }
}

