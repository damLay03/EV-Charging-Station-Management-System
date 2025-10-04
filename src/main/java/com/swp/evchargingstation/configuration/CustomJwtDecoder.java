package com.swp.evchargingstation.configuration;

import com.nimbusds.jwt.SignedJWT;
import com.swp.evchargingstation.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.singerKey}")
    private String singerKey;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    private JwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Parse the token to get the JWT ID
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();

            // Check if token is invalidated (logged out)
            if (invalidatedTokenRepository.existsById(jti)) {
                throw new JwtException("Token has been invalidated (logged out)");
            }
        } catch (ParseException e) {
            throw new JwtException("Invalid token format");
        }

        // If token is not invalidated, proceed with normal JWT validation
        if (nimbusJwtDecoder == null) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(singerKey.getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
