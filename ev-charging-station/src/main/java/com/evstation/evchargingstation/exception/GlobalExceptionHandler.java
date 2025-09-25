package com.evstation.evchargingstation.exception;

import com.evstation.evchargingstation.dto.request.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice //Khai báo đây là class handling exception
public class GlobalExceptionHandler {

    //Bắt exception runtime
    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRunTimeException(RuntimeException exception) {
        ApiResponse apiResponse = new ApiResponse<>();

        apiResponse.setCode(1001); //Code lỗi đã được quy định trong ApiResponse
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(apiResponse); //nội dung mình mốn trả về cho user
    }

    //Bắt exception validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<String> handlingValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(exception.getFieldError().getDefaultMessage());
    }
}
