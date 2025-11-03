package com.swp.evchargingstation.controller;

import com.nimbusds.jose.JOSEException;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.request.AuthenticationRequest;
import com.swp.evchargingstation.dto.request.IntrospectRequest;
import com.swp.evchargingstation.dto.response.AuthenticationResponse;
import com.swp.evchargingstation.dto.response.IntrospectResponse;
import com.swp.evchargingstation.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "API xác thực người dùng, đăng nhập, kiểm tra token")
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập người dùng",
            description = "API cho phép người dùng đăng nhập bằng email và mật khẩu. Trả về JWT token nếu thành công.")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    @Operation(summary = "Kiểm tra tính hợp lệ của token",
            description = "API kiểm tra xem token JWT có hợp lệ, chưa hết hạn và có thể sử dụng được không.")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }
}
