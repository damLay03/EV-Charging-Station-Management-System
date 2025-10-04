package com.swp.evchargingstation.service;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.entity.Admin;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.AdminRepository;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.StaffRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.swp.evchargingstation.mapper.UserMapper;
import com.swp.evchargingstation.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder; //nhan passwordEncoder tu SecurityConfig

    // Thêm các repository cho Driver, Staff, Admin
    DriverRepository driverRepository;
    StaffRepository staffRepository;
    AdminRepository adminRepository;

    //encoder đã tạo ở SecurityConfig, khong can dua vao thu vien nua
//    @Bean
//    public PasswordEncoder passwordEncoder()
//    {
//        return new BCryptPasswordEncoder();
//    }

    public UserResponse register(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        // Map user voi user request
        User user = userMapper.toUser(request);

//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10); BO LUON KHONG CAN NUA

        // ma hoa password
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        //set role dua vao email
        String email = request.getEmail().toLowerCase();
        user.setRole(Role.DRIVER);
        createRoleSpecificRecord(user);
        //luu user vao db
        return userMapper.toUserResponse(userRepository.save(user));
    }

    //Tạo bản ghi trong bảng driver/staff/admin tương ứng với role của user
    private void createRoleSpecificRecord(User user) {
        switch (user.getRole()) {
            case DRIVER:
                Driver driver = Driver.builder()
                        .userId(user.getUserId())
                        .user(user)
                        .address(null) // Có thể để null, user sẽ cập nhật sau
                        .build();
                driverRepository.save(driver);
                log.info("Driver record created for user ID: {}", user.getUserId());
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

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    // SELF UPDATE SAU KHI DANG KY: user bo sung cac thong tin bat buoc (phone, dateOfBirth, gender, firstName, lastName)
    // Khong cho phep doi email, password, role.
    public UserResponse updateMyInfo(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String principalEmail = context.getAuthentication().getName();
        User currentUser = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userMapper.updateUser(currentUser, request); // chi update field duoc phep
        return userMapper.toUserResponse(userRepository.save(currentUser));
    }

    // ALIAS (theo yeu cau doi ten updateUser). KHONG thay doi logic: van chi cho phep user tu update ho so cua minh
    // NOTE gender mapping moi: true = female, false = male (giu nguyen field boolean, UI tu dien giai)
    public UserResponse updateUser(UserUpdateRequest request) {
        return updateMyInfo(request); // delegate
    }

    //lay tat ca user
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
