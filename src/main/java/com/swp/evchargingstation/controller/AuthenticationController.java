package com.swp.evchargingstation.controller;

import com.nimbusds.jose.JOSEException;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.AuthenticationRequest;
import com.swp.evchargingstation.dto.request.IntrospectRequest;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.dto.response.IntrospectResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Authentication Management", description = "RESTful API xác thực (Login, OAuth2 Google, Token)")
public class AuthenticationController {

    AuthenticationService authenticationService;
    UserRepository userRepository;
    DriverRepository driverRepository;
    PasswordEncoder passwordEncoder;

    // ==================== EMAIL/PASSWORD LOGIN ====================

    @PostMapping("/login")
    @Operation(
            summary = "[PUBLIC] Đăng nhập người dùng",
            description = "API cho phép người dùng đăng nhập bằng email và mật khẩu. Trả về JWT token nếu thành công"
    )
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    @Operation(
            summary = "[PUBLIC] Kiểm tra tính hợp lệ của token",
            description = "API kiểm tra xem token JWT có hợp lệ, chưa hết hạn và có thể sử dụng được không"
    )
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    // ==================== GOOGLE OAUTH2 LOGIN ====================

    @GetMapping("/google/callback")
    @Operation(
            summary = "[PUBLIC] Google OAuth2 callback",
            description = "Handle Google login callback and return JWT token"
    )
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

