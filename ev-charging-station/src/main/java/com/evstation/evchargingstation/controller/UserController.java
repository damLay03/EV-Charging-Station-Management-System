package com.evstation.evchargingstation.controller;

import com.evstation.evchargingstation.dto.request.UserCreationRequest;
import com.evstation.evchargingstation.dto.request.UserLoginRequest;
import com.evstation.evchargingstation.dto.request.UserUpdateRequest;
import com.evstation.evchargingstation.dto.response.UserResponse;
import com.evstation.evchargingstation.entity.User;
import com.evstation.evchargingstation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth") //Khi mình dùng thằng endpoint users nhiều thì mình bế nó lên đây luôn
public class UserController {
    @Autowired //cái này giống như mình new vậy á
    private UserService userService;

    //Dang nhap (chua chuan)
    @PostMapping("/login")
    public String login(@RequestBody UserLoginRequest loginRequest) {
        boolean success = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        if (success) {
            return "Login successful!";
        }
        return "Invalid email or password!";
    }

    //Dang ki (chua chuan)
    @PostMapping("/register")
    public String register(@RequestBody @Valid UserCreationRequest request) {
        return userService.register(request);
    }

    //Tạo ra một user mới với id tự generate
    @PostMapping
    User createUser(@RequestBody UserCreationRequest newUser) {
        return userService.createUser(newUser);
    }

    //Update một user
    @PutMapping("/{userId}")
    UserResponse updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest updateRequest) {
        return userService.updateUser(userId, updateRequest);
    }

    //Lấy một user ra bằng id //moi cap nhat User -> UserResponse
    @GetMapping("/{userId}") //đây là một cái path
    UserResponse getUser(@PathVariable("userId") String userId) {
        return userService.getUser(userId);
    }

    //Lấy toàn bộ thông tin users hiện có
    @GetMapping
    List<User> userList() {
        return userService.getUser();
    }
}
