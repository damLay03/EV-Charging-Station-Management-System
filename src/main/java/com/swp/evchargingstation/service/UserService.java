package com.swp.evchargingstation.service;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.response.admin.AdminUserResponse;
import com.swp.evchargingstation.dto.response.driver.DriverResponse;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.swp.evchargingstation.mapper.UserMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder; //nhan passwordEncoder tu SecurityConfig
    SubscriptionRepository subscriptionRepository;
    ChargingSessionRepository chargingSessionRepository;

    DriverRepository driverRepository;
    StaffRepository staffRepository;
    AdminRepository adminRepository;

    //encoder đã tạo ở SecurityConfig, khong can dua vao thu vien nua
//    @Bean
//    public PasswordEncoder passwordEncoder()
//    {
//        return new BCryptPasswordEncoder();
//    }

//=====================================================DRIVER===========================================================

    public UserResponse register(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        // Map user voi user request
        User user = userMapper.toUser(request);
        //PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10); BO LUON KHONG CAN NUA
        // ma hoa password
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        //set role dua vao email
        String email = request.getEmail().toLowerCase();
        user.setRole(Role.DRIVER);
        createRoleSpecificRecord(user);
        //luu user vao db
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('DRIVER')")
    public DriverResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lấy thông tin Driver entity
        Driver driver = driverRepository.findById(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Map sang DriverResponse với đầy đủ thông tin
        return DriverResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.isGender())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .address(driver.getAddress())
                .joinDate(driver.getJoinDate())
                .build();
    }

    // SELF UPDATE SAU KHI DANG KY: user bo sung cac thong tin bat buoc
    // Khong cho phep doi email, password, role.
    @PreAuthorize("hasRole('DRIVER')")
    public DriverResponse updateMyInfo(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String principalEmail = context.getAuthentication().getName();
        User currentUser = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Update User fields
        userMapper.updateUser(currentUser, request);
        userRepository.save(currentUser);

        // Update Driver fields (address)
        if (request.getAddress() != null) {
            Driver driver = driverRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            driver.setAddress(request.getAddress());
            driverRepository.save(driver);
        }

        // Return updated info
        return getMyInfo();
    }

    // ALIAS (theo yeu cau doi ten updateUser). KHONG thay doi logic: van chi cho phep user tu update ho so cua minh
    // NOTE gender mapping moi: true = female, false = male (giu nguyen field boolean, UI tu dien giai)
    public DriverResponse updateUser(UserUpdateRequest request) {
        return updateMyInfo(request); // delegate
    }

    private void createRoleSpecificRecord(User user) {
        switch (user.getRole()) {
            case DRIVER:
                Driver driver = Driver.builder()
                        .userId(user.getUserId())
                        .user(user)
                        .address(null) // Có thể để null, user sẽ cập nhật sau
                        .joinDate(LocalDateTime.now()) // Lưu mốc thời gian đăng ký
                        .build();
                driverRepository.save(driver);
                log.info("Driver record created for user ID: {} at {}", user.getUserId(), driver.getJoinDate());
                break;

            case STAFF:
                Staff staff = Staff.builder()
                        .userId(user.getUserId())
                        .user(user)
                        .employeeNo(null) // Có thể để null hoặc generate mã nhân viên
                        .position(null) // Có thể để null, admin sẽ cập nhật sau
                        .build();
                staffRepository.save(staff);
                log.info("Staff record created for user ID: {}", user.getUserId());
                break;

            case ADMIN:
                Admin admin = Admin.builder()
                        .userId(user.getUserId())
                        .user(user)
                        .build();
                adminRepository.save(admin);
                log.info("Admin record created for user ID: {}", user.getUserId());
                break;

            default:
                log.warn("Unknown role: {}", user.getRole());
        }
    }

//=====================================================STAFF============================================================

//=====================================================ADMIN============================================================

    //lay tat ca driver cho ADMIN voi day du thong tin (ten, lien he, ngay tham gia, goi dich vu, so phien, tong chi tieu, trang thai)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserResponse> getDriversForAdmin() {
        log.info("In method get Drivers For Admin");
        return driverRepository.findAllWithUser() // Lay truc tiep tu DriverRepository
                .stream()
                .map(driver -> mapToAdminUserResponse(driver))
                .collect(Collectors.toList());
    }

    // NOTE: ADMIN update thông tin driver theo userId. Không được phép sửa email, password, joinDate.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DriverResponse updateDriverByAdmin(String driverId, AdminUpdateDriverRequest request) {
        log.info("Admin updating driver id='{}'", driverId);

        // Tìm user theo id
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user có phải là DRIVER không
        if (user.getRole() != Role.DRIVER) {
            throw new AppException(ErrorCode.USER_NOT_FOUND); // hoặc tạo error code mới: NOT_A_DRIVER
        }

        // Update User fields (chỉ update nếu không null)
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        userRepository.save(user);

        // Update Driver fields (address)
        if (request.getAddress() != null) {
            Driver driver = driverRepository.findById(driverId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            driver.setAddress(request.getAddress());
            driverRepository.save(driver);
        }

        log.info("Admin updated driver '{}' successfully", driverId);

        // Return updated info
        return getDriverInfo(driverId);
    }

    // NOTE: Helper method để lấy thông tin driver theo ID (dùng chung cho admin)
    @PreAuthorize("hasRole('ADMIN')")
    public DriverResponse getDriverInfo(String driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user có phải là DRIVER không
        if (user.getRole() != Role.DRIVER) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return DriverResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.isGender())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .address(driver.getAddress())
                .joinDate(driver.getJoinDate())
                .build();
    }

    // Helper method: map Driver sang AdminUserResponse
    private AdminUserResponse mapToAdminUserResponse(Driver driver) {
        User user = driver.getUser();
        String planName = null;
        Integer sessionCount = 0;
        Double totalSpent = 0.0;

        // Lay thong tin subscription (goi dich vu)
        Optional<Subscription> activeSubscription = subscriptionRepository
                .findActiveSubscriptionByDriverId(user.getUserId());
        if (activeSubscription.isPresent() && activeSubscription.get().getPlan() != null) {
            planName = activeSubscription.get().getPlan().getName();
        }

        // Lay so phien sac
        sessionCount = chargingSessionRepository.countByDriverId(user.getUserId());
        if (sessionCount == null) {
            sessionCount = 0;
        }

        // Lay tong chi tieu
        totalSpent = chargingSessionRepository.sumTotalSpentByDriverId(user.getUserId());
        if (totalSpent == null) {
            totalSpent = 0.0;
        }

        // Lay joinDate tu Driver entity - chuyen LocalDateTime sang LocalDate
        LocalDate joinDate = null;
        if (driver.getJoinDate() != null) {
            joinDate = driver.getJoinDate().toLocalDate();
        }

        return AdminUserResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .joinDate(joinDate) // Lay tu Driver entity
                .planName(planName)
                .sessionCount(sessionCount)
                .totalSpent(totalSpent)
                .status("Hoạt động") // Mac dinh "Hoat dong", co the customize sau
                .isActive(true) // Mac dinh true, co the customize sau
                .build();
    }

    //lay tat ca user (legacy method, giu nguyen de khong break existing code)
    //moi cap nhat (User -> UserResponse, map tung User thanh UserResponse)
    @PreAuthorize("hasRole('ADMIN')") //chi co ADMIN moi truy cap duoc
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    //lay user theo id
    //moi cap nhat User -> UserResponse
    @PreAuthorize("returnObject.email == authentication.name or hasAuthority('ROLE_ADMIN')") //user chi xem duoc thong tin cua minh, hoac admin xem duoc tat ca
    public UserResponse getUser(String id) {
        log.info("In method get user by id");
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    // NOTE: ADMIN xóa user theo id (hard delete). Nếu không tìm thấy -> USER_NOT_FOUND.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user); // hard delete
        log.info("Deleted user id={}", id);
    }
}
