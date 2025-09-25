package com.evstation.evchargingstation.controller;

import com.evstation.evchargingstation.dto.request.UserCreationRequest;
import com.evstation.evchargingstation.dto.UserLoginRequest;
import com.evstation.evchargingstation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth") //Khi mình dùng thằng endpoint users nhiều thì mình bế nó lên đây luôn
public class UserController {
    @Autowired //cái này giống như mình new vậy á
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody UserLoginRequest loginRequest) {
        boolean success = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        if (success) {
            return "Login successful!";
        }
        return "Invalid email or password!";
    }

    //Tại sao lại là String
    @PostMapping("/register")
    public String register(@RequestBody @Valid UserCreationRequest request) {
        return userService.register(request);
    }

//    //Tạo ra một user mới với id tự generate
//    @PostMapping
//    User createUser(@RequestBody UserCreationRequest newUser) {
//        return userService.createUser(newUser);
//    }
//    //Lấy toàn bộ thông tin users hiện có
//    @GetMapping
//    List<User> userList() {
//        return userService.getUser();
//    }
//    //Lấy một user ra bằng id
//    @GetMapping("/{userId}") //đây là một cái path
//    User getUser(@PathVariable("userId") String userId) {
//        return userService.getUser(userId);
//    }
//    //Update một user
//    @PutMapping("/{uerId}")
//    User updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest updateRequest) {
//        return userService.updateUser(userId, updateRequest);
//    }
}
