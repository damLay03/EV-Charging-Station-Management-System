package com.swp.evchargingstation.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.entity.User;
import lombok.experimental.NonFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    UserRepository userRepository;
    @NonFinal
    protected static final String SIGN_KEY = "0a58c8b134bc3d3e7a853dc8a49bcd3895e02c20d39d29d2d976e87300dc23fa";

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
            jwsObject.sign(new MACSigner(SIGN_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Canot sign the token", e);
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
}
