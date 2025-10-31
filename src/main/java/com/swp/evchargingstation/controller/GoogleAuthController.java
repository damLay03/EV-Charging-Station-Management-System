package com.swp.evchargingstation.controller;

import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Google Authentication", description = "Google OAuth2 Login APIs")
public class GoogleAuthController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/google/callback")
    @Operation(summary = "Google OAuth2 callback",
               description = "Handle Google login callback and return JWT token")
    @Transactional
    public ApiResponse<AuthenticationResponse> googleCallback(@AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        log.info("Google login attempt: email={}, name={}", email, name);

        // Tìm user theo email hoặc googleId
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByGoogleId(googleId)
                        .orElse(null));

        // Nếu chưa có user -> tạo mới
        if (user == null) {
            log.info("Creating new user from Google account: {}", email);

            // Tách firstName và lastName từ name
            String firstName = "User";
            String lastName = "";

            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                firstName = nameParts.length > 0 ? nameParts[0] : "User";
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }

            user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .googleId(googleId)
                    .password(passwordEncoder.encode("GOOGLE_OAUTH_" + googleId)) // Random password
                    .role(Role.DRIVER) // Mặc định là DRIVER
                    .build();
            user = userRepository.save(user);

            // Tạo Driver profile
            Driver driver = Driver.builder()
                    .user(user)
                    .joinDate(LocalDateTime.now())
                    .build();
            driver.setUserId(user.getUserId());
            driverRepository.save(driver);

            log.info("Created new user and driver profile: {}", user.getUserId());

        } else if (user.getGoogleId() == null) {
            // User đã tồn tại nhưng chưa link Google -> link
            log.info("Linking existing user {} to Google account", email);
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        // Tạo JWT token
        String token = authenticationService.generateToken(user);

        log.info("Google login successful: userId={}, role={}", user.getUserId(), user.getRole());

        return ApiResponse.<AuthenticationResponse>builder()
                .result(AuthenticationResponse.builder()
                        .token(token)
                        .authenticated(true)
                        .build())
                .build();
    }
}

