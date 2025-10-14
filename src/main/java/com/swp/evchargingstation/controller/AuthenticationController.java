package com.swp.evchargingstation.controller;

import com.nimbusds.jose.JOSEException;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.AuthenticationRequest;
import com.swp.evchargingstation.dto.request.IntrospectRequest;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.dto.response.IntrospectResponse;
import com.swp.evchargingstation.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }
}
