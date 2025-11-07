package com.swp.evchargingstation.service;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.request.AdminUpdateDriverRequest;
import com.swp.evchargingstation.dto.request.RoleAssignmentRequest;
import com.swp.evchargingstation.dto.response.*;
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

    @Transactional
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
        //set role mặc định là DRIVER
        user.setRole(Role.DRIVER);
        // Lưu user trước khi tạo record role-specific để đảm bảo userId đã sinh
        User saved = userRepository.save(user);
        createRoleSpecificRecord(saved);
        // trả về response từ user đã lưu
        return userMapper.toUserResponse(saved);
    }

    // NEW: Support all 3 roles - returns unified profile response
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return switch (user.getRole()) {
            case DRIVER -> {
                Driver driver = driverRepository.findById(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                DriverResponse driverProfile = DriverResponse.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .dateOfBirth(user.getDateOfBirth())
                        .gender(user.getGender())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .address(driver.getAddress())
                        .joinDate(driver.getJoinDate())
                        .build();

                yield UserProfileResponse.builder()
                        .role(Role.DRIVER)
                        .driverProfile(driverProfile)
                        .build();
            }
            case STAFF -> {
                Staff staff = staffRepository.findById(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                StaffProfileResponse staffProfile = StaffProfileResponse.builder()
                        .staffId(user.getUserId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .employeeNo(staff.getEmployeeNo())
                        .position(staff.getPosition())
                        .stationId(staff.getStation() != null ? staff.getStation().getStationId() : null)
                        .stationName(staff.getStation() != null ? staff.getStation().getName() : null)
                        .stationAddress(staff.getStation() != null ? staff.getStation().getAddress() : null)
                        .build();

                yield UserProfileResponse.builder()
                        .role(Role.STAFF)
                        .staffProfile(staffProfile)
                        .build();
            }
            case ADMIN -> {
                Admin admin = adminRepository.findById(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                AdminProfileResponse adminProfile = AdminProfileResponse.builder()
                        .adminId(user.getUserId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .dateOfBirth(user.getDateOfBirth())
                        .gender(user.getGender())
                        .department(null) // Admin entity doesn't have department field yet
                        .build();

                yield UserProfileResponse.builder()
                        .role(Role.ADMIN)
                        .adminProfile(adminProfile)
                        .build();
            }
        };
    }

    // DEPRECATED: Keep for backward compatibility, only works for DRIVER
    @Deprecated
    @PreAuthorize("hasRole('DRIVER')")
    public DriverResponse getMyInfoLegacy() {
        UserProfileResponse profile = getMyInfo();
        return profile.getDriverProfile();
    }

    // SELF UPDATE: user bo sung cac thong tin bat buoc
    // Khong cho phep doi email, password, role.
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse updateMyInfo(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String principalEmail = context.getAuthentication().getName();
        User currentUser = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Update User fields (common for all roles)
        userMapper.updateUser(currentUser, request);
        userRepository.save(currentUser);

        // Update role-specific fields
        switch (currentUser.getRole()) {
            case DRIVER -> {
                if (request.getAddress() != null) {
                    Driver driver = driverRepository.findById(currentUser.getUserId())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    driver.setAddress(request.getAddress());
                    driverRepository.save(driver);
                }
            }
            case STAFF -> {
                // Staff có thể cập nhật position, employeeNo nếu cần
                // Hiện tại chỉ update User fields
                log.info("Staff {} updated profile", currentUser.getUserId());
            }
            case ADMIN -> {
                // Admin có thể cập nhật department nếu cần
                // Hiện tại chỉ update User fields
                log.info("Admin {} updated profile", currentUser.getUserId());
            }
        }

        // Return updated info
        return getMyInfo();
    }

    // DEPRECATED: Keep for backward compatibility
    @Deprecated
    public DriverResponse updateUser(UserUpdateRequest request) {
        UserProfileResponse profile = updateMyInfo(request);
        if (profile.getDriverProfile() != null) {
            return profile.getDriverProfile();
        }
        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private void createRoleSpecificRecord(User user) {
        switch (user.getRole()) {
            case DRIVER: {
                Driver driver = Driver.builder()
                        // Không set userId thủ công khi dùng @MapsId
                        .user(user)
                        .address(null)
                        .joinDate(LocalDateTime.now())
                        .build();
                driverRepository.save(driver);
                log.info("Driver record created for user ID: {} at {}", user.getUserId(), driver.getJoinDate());
                break;
            }
            case STAFF: {
                Staff staff = Staff.builder()
                        // Không set userId thủ công khi dùng @MapsId
                        .user(user)
                        .employeeNo(null)
                        .position(null)
                        .build();
                staffRepository.save(staff);
                log.info("Staff record created for user ID: {}", user.getUserId());
                break;
            }
            case ADMIN: {
                Admin admin = Admin.builder()
                        // Không set userId thủ công khi dùng @MapsId
                        .user(user)
                        .build();
                adminRepository.save(admin);
                log.info("Admin record created for user ID: {}", user.getUserId());
                break;
            }
            default:
                log.warn("Unknown role: {}", user.getRole());
        }
    }

    // Đảm bảo tồn tại bản ghi role-specific tương ứng với user.role; nếu thiếu thì tạo mới (idempotent)
    //Dùng khi gán role cho user trùng với lại role đã tồn tại sẵn của user đó
    private void ensureRoleSpecificRecord(User user) {
        switch (user.getRole()) {
            case DRIVER:
                if (driverRepository.findById(user.getUserId()).isEmpty()) {
                    log.info("Driver record missing for user {}. Creating...", user.getUserId());
                    createRoleSpecificRecord(user);
                }
                break;
            case STAFF:
                if (staffRepository.findById(user.getUserId()).isEmpty()) {
                    log.info("Staff record missing for user {}. Creating...", user.getUserId());
                    createRoleSpecificRecord(user);
                }
                break;
            case ADMIN:
                if (adminRepository.findById(user.getUserId()).isEmpty()) {
                    log.info("Admin record missing for user {}. Creating...", user.getUserId());
                    createRoleSpecificRecord(user);
                }
                break;
            default:
                log.warn("Unknown role when ensuring record: {} for user {}", user.getRole(), user.getUserId());
        }
    }

//=====================================================STAFF============================================================

    /**
     * [ADMIN] Lấy thông tin chi tiết một staff cụ thể
     */
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse getStaffInfo(String staffId) {
        log.info("Admin fetching staff info: {}", staffId);

        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user có phải là STAFF không
        if (user.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.STAFF_NOT_FOUND);
        }

        Staff staff = staffRepository.findByIdWithStation(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Lấy thông tin trạm được quản lý
        String managedStationId = null;
        String managedStationName = null;
        if (staff.getManagedStation() != null) {
            managedStationId = staff.getManagedStation().getStationId();
            managedStationName = staff.getManagedStation().getName();
        }

        return StaffResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .employeeNo(staff.getEmployeeNo())
                .position(staff.getPosition())
                .managedStationId(managedStationId)
                .managedStationName(managedStationName)
                .build();
    }

    /**
     * [ADMIN] Cập nhật thông tin staff
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StaffResponse updateStaffByAdmin(String staffId, com.swp.evchargingstation.dto.request.AdminUpdateStaffRequest request) {
        log.info("Admin updating staff id='{}'", staffId);

        // Tìm user theo id
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user có phải là STAFF không
        if (user.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.STAFF_NOT_FOUND);
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

        // Update Staff fields (employeeNo, position)
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (request.getEmployeeNo() != null) {
            staff.setEmployeeNo(request.getEmployeeNo());
        }
        if (request.getPosition() != null) {
            staff.setPosition(request.getPosition());
        }

        staffRepository.save(staff);

        log.info("Admin updated staff '{}' successfully", staffId);

        // Return updated info
        return getStaffInfo(staffId);
    }

    /**
     * Helper method: map Staff sang AdminUserResponse
     */
    private AdminUserResponse mapStaffToAdminUserResponse(Staff staff) {
        User user = staff.getUser();

        // Lấy thông tin trạm
        String stationName = null;
        if (staff.getManagedStation() != null) {
            stationName = staff.getManagedStation().getName();
        }

        return AdminUserResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .joinDate(null) // Staff không có joinDate như Driver
                .planName(stationName) // Dùng để hiển thị trạm quản lý
                .sessionCount(null) // Staff không có session count
                .totalSpent(null) // Staff không có total spent
                .status("Hoạt động")
                .isActive(true)
                .build();
    }

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
                .gender(user.getGender())
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
        Integer sessionCount = 0;
        Double totalSpent = 0.0;

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
                .planName(null) // Removed subscription support
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

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse assignRoleToUser(String userId, RoleAssignmentRequest request) {
        log.info("Admin assigning role {} to user {}", request.getRole(), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Role newRole = request.getRole();
        Role oldRole = user.getRole();

        if (oldRole == newRole) {
            log.info("User {} already has role {}. Ensuring role-specific record exists...", userId, newRole);
            ensureRoleSpecificRecord(user);
            return userMapper.toUserResponse(user);
        }

        // Remove old role-specific record if exists
        if (oldRole != null) {
            switch (oldRole) {
                case DRIVER:
                    driverRepository.findById(userId).ifPresent(driverRepository::delete);
                    log.info("Removed Driver record for user {}", userId);
                    break;
                case STAFF:
                    staffRepository.findById(userId).ifPresent(staffRepository::delete);
                    log.info("Removed Staff record for user {}", userId);
                    break;
                case ADMIN:
                    adminRepository.findById(userId).ifPresent(adminRepository::delete);
                    log.info("Removed Admin record for user {}", userId);
                    break;
                default:
                    log.warn("Unknown old role {} for user {}", oldRole, userId);
            }
        }

        // Assign new role and create corresponding record
        user.setRole(newRole);
        userRepository.save(user);
        createRoleSpecificRecord(user);

        return userMapper.toUserResponse(user);
    }
}
