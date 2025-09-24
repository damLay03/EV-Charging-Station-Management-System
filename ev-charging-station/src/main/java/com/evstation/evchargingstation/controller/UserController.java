package com.evstation.evchargingstation.controller;

import com.evstation.evchargingstation.dto.UserCreationRequest;
import com.evstation.evchargingstation.dto.UserUpdateRequest;
import com.evstation.evchargingstation.entity.User;
import com.evstation.evchargingstation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users") //Khi mình dùng thằng endpoint users nhiều thì mình bế nó lên đây luôn
public class UserController {
    @Autowired //cái này giống như mình new vậy á
    private UserService userService;

    //Tạo ra một user mới với id tự generate
    @PostMapping
    User createUser(@RequestBody UserCreationRequest newUser) {
        return userService.createUser(newUser);
    }
    //Lấy toàn bộ thông tin users hiện có
    @GetMapping
    List<User> userList() {
        return userService.getUser();
    }
    //Lấy một user ra bằng id
    @GetMapping("/{userId}") //đây là một cái path
    User getUser(@PathVariable("userId") String userId) {
        return userService.getUser(userId);
    }
    //Update một user
    @PutMapping("/{uerId}")
    User updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest updateRequest) {
        return userService.updateUser(userId, updateRequest);
    }
}
