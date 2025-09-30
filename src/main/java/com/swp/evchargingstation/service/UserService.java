package com.swp.evchargingstation.service;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.swp.evchargingstation.mapper.UserMapper;
import com.swp.evchargingstation.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder; //nhan passwordEncoder tu SecurityConfig

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
        if (email.endsWith("@admin.ev.com")) {
            user.setRole(Role.ADMIN);
        } else if (email.endsWith("@staff.ev.com")) {
            user.setRole(Role.STAFF);
        } else {
            user.setRole(Role.DRIVER);
        }
        //luu user vao db
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        // Map user voi user request
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        //cap nhat user
        userMapper.updateUser(user, request);
        //luu user vao db va tra ve UserResponse
        return userMapper.toUserResponse(userRepository.save(user));
    }

    //moi cap nhat User -> UserResponse
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    //lay tat ca user
    //moi cap nhat (User -> UserResponse, map tung User thanh UserResponse)
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }
}
