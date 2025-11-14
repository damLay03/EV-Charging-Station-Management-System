package com.swp.evchargingstation.configuration;

import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthConfig extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        try {
            log.info("=== OAuth2 Login Success Handler START ===");

            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String googleId = oauth2User.getAttribute("sub");

            log.info("🔐 Google login attempt: email={}, name={}, googleId={}", email, name, googleId);

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.findByGoogleId(googleId)
                            .orElse(null));

            if (user == null) {
                log.info("Creating new user from Google account: {}", email);

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
                        .password(passwordEncoder.encode("GOOGLE_OAUTH_" + googleId))
                        .role(Role.DRIVER)
                        .build();
                user = userRepository.save(user);
                log.info("User saved with ID: {}", user.getUserId());

                Driver driver = Driver.builder()
                        .user(user)
                        .joinDate(LocalDateTime.now())
                        .build();
                driverRepository.save(driver);

                log.info("Created new user and driver profile: {}", user.getUserId());

            } else if (user.getGoogleId() == null) {
                log.info("Linking existing user {} to Google account", email);
                user.setGoogleId(googleId);
                userRepository.save(user);
            }

            String token = authenticationService.generateToken(user);

            log.info("🎉 Google login successful: userId={}, role={}", user.getUserId(), user.getRole());

            // Xác định frontend URL dựa trên request origin
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");

            String frontendUrl;
            if (origin != null && (origin.contains("web.khoahtd.id.vn") || origin.contains("evchargingstation.khoahtd.id.vn"))) {
                frontendUrl = "https://web.khoahtd.id.vn";
            } else if (referer != null && (referer.contains("web.khoahtd.id.vn") || referer.contains("evchargingstation.khoahtd.id.vn"))) {
                frontendUrl = "https://web.khoahtd.id.vn";
            } else {
                // Fallback to localhost for development
                frontendUrl = "http://localhost:5173";
            }

            String targetUrl = frontendUrl + "/auth/google/callback?token=" + token;
            log.info("🔄 Redirecting to: {}", targetUrl);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

            log.info("=== OAuth2 Login Success Handler END ===");

        } catch (Exception e) {
            log.error("❌ Error in OAuth2LoginSuccessHandler: ", e);

            // Xác định frontend URL cho error redirect
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            String frontendUrl = "http://localhost:5173";

            if ((origin != null && origin.contains("web.khoahtd.id.vn")) ||
                (referer != null && referer.contains("web.khoahtd.id.vn"))) {
                frontendUrl = "https://web.khoahtd.id.vn";
            }

            response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
        }
    }
}