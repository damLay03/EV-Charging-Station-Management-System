package com.swp.evchargingstation.exception;

public enum ErrorCode {
    UNAUTHORIZED_EXCEPTION(3979,"Unauthorized Exception"),
    USER_EXISTED(1001,"User Existed"),
    PASSWORD_NOT_MATCH(1002,"Password Not Match"),
    EMAIL_EXISTED(1003,"Email Existed"),
    USER_NOT_FOUND(1004,"User Not Found"),
    UNAUTHENTICATED(1005,"Unauthenticated"),
    VALIDATION_FAILED(1006, "Validation Failed"), // generic validation error
    STATION_NOT_FOUND(2001, "Station Not Found") // added for station module
    ;

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
