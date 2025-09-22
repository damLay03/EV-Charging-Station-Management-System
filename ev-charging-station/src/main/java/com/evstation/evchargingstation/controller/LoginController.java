package com.evstation.evchargingstation.controller;
import com.evstation.evchargingstation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        boolean success = userService.login(username, password);
        if (success) {
            return "Login successful!";
        }
        return "Invalid username or password!";
    }
}
