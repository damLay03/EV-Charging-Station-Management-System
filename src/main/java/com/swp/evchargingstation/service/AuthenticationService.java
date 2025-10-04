package com.swp.evchargingstation.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swp.evchargingstation.dto.request.LogoutRequest;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.entity.InvalidatedToken;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.repository.InvalidatedTokenRepository;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.swp.evchargingstation.dto.request.AuthenticationRequest;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
//    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class); //da co @Slf4j, khong can nua
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    //use YAML config instead
    // protected static final String SIGN_KEY = "0a58c8b134bc3d3e7a853dc8a49bcd3895e02c20d39d29d2d976e87300dc23fa";

    //using YAML configuration
    @Value("${jwt.singerKey}")
    @NonFinal
    private String singerKey;

    //update lai phuong thuc authenticate (SecurityConfig)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("ev-charging-station")
                .issueTime(new Date())
                .expirationTime(new Date(//new Date().getTime() + 60 * 60 * 1000)) // 1 hour expiration
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString()) // Add JWT ID for token tracking
                .claim("scope", buildScope(user)) //scope chua thong tin ve vai tro cua user
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        //Cai nay nam trong thu vien nimbus-jose-jwt, de tao va kiem tra JWT
        //can truyen vao header va payload
        //header: chua thong tin ve kieu token, phuong thuc kiem tra
        //payload: chua thong tin ve user, thoi gian het han, vai tro, ...
        JWSObject jwsObject = new JWSObject(header, payload);

        //ki token bang key
        try {
            //jwsObject.sign(new MACSigner(SIGN_KEY.getBytes())); //khong xai sign key (code cung ngat)
            //use singerKey from YAML
            jwsObject.sign(new MACSigner(singerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot sign the token", e);
            throw new RuntimeException(e);
        }
    }

    //lay thong tin role cua user de dua vao scope
    private String buildScope(User user){
        StringJoiner stringJoiner = new StringJoiner(" "); //scope quy dinh phan cach bang dau cach
        if (user.getRole() != null) {
            stringJoiner.add(user.getRole().toString());
        }
        return stringJoiner.toString();
    }

    //logout: invalidate token by adding it to the invalidated token list
    public void logout(LogoutRequest request) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Token invalidated successfully: {}", jti);
        } catch (ParseException e) {
            log.error("Error parsing token for logout", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
