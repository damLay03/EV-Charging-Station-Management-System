package com.evstation.evchargingstation.controller;

import com.evstation.evchargingstation.entity.User;
import com.evstation.evchargingstation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody User loginRequest) {
        boolean success = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (success) {
            return "Login successful!";
        }
        return "Invalid username or password!";
    }
}
