package com.evstation.evchargingstation.service;
import com.evstation.evchargingstation.dto.request.UserCreationRequest;
import com.evstation.evchargingstation.dto.request.UserUpdateRequest;
import com.evstation.evchargingstation.dto.response.UserResponse;
import com.evstation.evchargingstation.entity.User;
import com.evstation.evchargingstation.enums.Role;
import com.evstation.evchargingstation.exception.AppException;
import com.evstation.evchargingstation.exception.ErrorCode;
import com.evstation.evchargingstation.mapper.UserMapper;
import com.evstation.evchargingstation.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    //@Autowired //co @RequiredArgsConstructor roi thi khong can thiet phai @Autowired nua
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    // Đã thay đổi bên repo, find user bên đây luôn nha!!!!
    // login user
    public boolean login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return true;
        }
        return false;
    }

    // register user
    public String register(UserCreationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        userRepository.save(user);

        return "Registration successful!";
    }

    public User createUser(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Vi co map roi nen khong can thiet phai set tung thu nhu ben duoi nua (doc xong roi co the xoa luon)
//        user.setEmail(request.getEmail());
//        user.setPassword(request.getPassword());
//        user.setPhone(request.getPhone());
//        user.setFullName(request.getFullName());
//        user.setRole(request.getRole());

        // Map user voi user request
        User user = userMapper.toUser(request);

        // ma hoa password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //set role mac dinh la DRIVER
        HashSet<String> roles = new HashSet<>();
        roles.add(Role.DRIVER.name());
        user.setRoles(roles);

        //luu user vao db
        return userRepository.save(user);
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        // Map user voi user request
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        //
        userMapper.updateUser(user, request);

        // Vi co map roi nen khong can thiet phai set tung thu nhu ben duoi nua (doc xong roi co the xoa luon)
//        user.setEmail(updateRequest.getEmail());
//        user.setPassword(updateRequest.getPassword());
//        user.setPhone(updateRequest.getPhone());
//        user.setFullName(updateRequest.getFullName());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    //moi cap nhat User -> UserResponse
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id)));
    }

    public List<User> getUser() {
        return userRepository.findAll();
    }
}
