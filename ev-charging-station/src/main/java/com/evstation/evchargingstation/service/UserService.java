package com.evstation.evchargingstation.service;
import com.evstation.evchargingstation.dto.request.UserCreationRequest;
import com.evstation.evchargingstation.dto.request.UserUpdateRequest;
import com.evstation.evchargingstation.entity.User;
import com.evstation.evchargingstation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    // Đã thay đổi bên repo, find user bên đây luôn nha!!!!
    // login user
    public boolean login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return true;
        }
        return false;
    }

//    //Cai nay khong can den, vi da co ham register
//    public User createUser(UserCreationRequest request) {
//        User user = new User();
//
//        user.setEmail(request.getEmail());
//        user.setPassword(request.getPassword());
//        user.setPhone(request.getPhone());
//        user.setFullName(request.getFullName());
//        user.setRole(request.getRole());
//
//        return userRepository.save(user);
//    }

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

    public List<User> getUser() {
        return userRepository.findAll();
    }

    public User getUser(String id) {
        return userRepository.findById(id).orElseThrow(new Supplier<RuntimeException>() {
            @Override
            public RuntimeException get() {
                return new RuntimeException("User not found");
            }
        });
    }

    public User updateUser(String userId, UserUpdateRequest updateRequest) {
        User user = new User();

        user.setEmail(updateRequest.getEmail());
        user.setPassword(updateRequest.getPassword());
        user.setPhone(updateRequest.getPhone());
        user.setFullName(updateRequest.getFullName());

        return userRepository.save(user);
    }
}
