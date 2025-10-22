package com.swp.evchargingstation.exception;

import com.swp.evchargingstation.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle Spring Security Access Denied (when user doesn't have required role)
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(403);
        apiResponse.setMessage("Access Denied: " + exception.getMessage());
        return ResponseEntity.status(403).body(apiResponse);
    }

    // Handle Spring Security Authentication failures (invalid/expired token)
    @ExceptionHandler(value = AuthenticationException.class)
    ResponseEntity<ApiResponse> handlingAuthenticationException(AuthenticationException exception) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(401);
        apiResponse.setMessage("Authentication Failed: " + exception.getMessage());
        return ResponseEntity.status(401).body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode()); //Code lỗi đã được quy định trong ApiResponse
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse); //nội dung mình mốn trả về cho user
    }

    //Bắt exception validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String enumkey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumkey);

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode()); //code lỗi đã được quy định trong ApiResponse
        apiResponse.setMessage(errorCode.getMessage()); //lấy message từ enum

        return ResponseEntity.badRequest().body(apiResponse); //nội dung mình mốn trả về cho user
    }

    // Bắt tất cả exception chưa xử lý (500 Internal Server Error)
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception) {
        log.error("Unhandled exception occurred: ", exception);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(500);
        apiResponse.setMessage("Internal Server Error: " + exception.getMessage());

        return ResponseEntity.status(500).body(apiResponse);
    }
}
