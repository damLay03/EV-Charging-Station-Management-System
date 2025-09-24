package com.evstation.evchargingstation.service;
import com.evstation.evchargingstation.dto.UserCreationRequest;
import com.evstation.evchargingstation.dto.UserUpdateRequest;
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
    public boolean login(String username, String password) {
//        User user = userRepository.findByUsername(username);
//        if (user != null && user.getPassword().equals(password)) {
//            return true;
//        }
        return false;
    }

    public User createUser(UserCreationRequest request) {
        User user = new User();

        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());

        return userRepository.save(user);
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
