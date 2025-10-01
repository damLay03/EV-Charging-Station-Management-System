package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.request.ApiResponse;
import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.service.AuthenticationService;
import com.swp.evchargingstation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserService userService;

    @PostMapping("/register")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Authentication: {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }
}
