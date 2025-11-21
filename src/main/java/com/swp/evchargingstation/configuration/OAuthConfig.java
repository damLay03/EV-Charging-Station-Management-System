package com.swp.evchargingstation.configuration;

import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Plan;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.PlanRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.service.AuthenticationService;
import com.swp.evchargingstation.service.WalletService;
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
    private final WalletService walletService;
    private final PlanRepository planRepository;

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

                // Find default plan "Linh hoạt"
                Plan defaultPlan = planRepository.findByNameIgnoreCase("Linh hoạt")
                        .orElse(null);

                if (defaultPlan != null) {
                    log.info("Found default plan 'Linh hoạt' for Google OAuth user");
                } else {
                    log.warn("Default plan 'Linh hoạt' not found for Google OAuth user");
                }

                Driver driver = Driver.builder()
                        .user(user)
                        .joinDate(LocalDateTime.now())
                        .plan(defaultPlan) // Assign default plan
                        .build();
                driverRepository.save(driver);

                log.info("Created new user and driver profile: {} with plan: {}",
                        user.getUserId(),
                        defaultPlan != null ? defaultPlan.getName() : "None");

                // Create wallet for the new Google OAuth user (linked via user_id)
                try {
                    walletService.createWallet(user);
                    log.info("✅ Wallet created for Google OAuth user: {}", user.getUserId());
                } catch (Exception e) {
                    log.error("❌ Failed to create wallet for Google OAuth user: {}", user.getUserId(), e);
                    // Don't throw exception, allow login to continue
                }

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