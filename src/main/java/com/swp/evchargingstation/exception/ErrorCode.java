package com.swp.evchargingstation.exception;

public enum ErrorCode {
    UNAUTHORIZED_EXCEPTION(3979,"Unauthorized Exception"),
    USER_EXISTED(1001,"User Existed"),
    PASSWORD_NOT_MATCH(1002,"Password Not Match"),
    EMAIL_EXISTED(1003,"Email Existed"),
    USER_NOT_FOUND(1004,"User Not Found"),
    UNAUTHENTICATED(1005,"Unauthenticated"),
    VALIDATION_FAILED(1006, "Validation Failed"), // generic validation error
    STATION_NOT_FOUND(2001, "Station Not Found"), // added for station module
    PLAN_NOT_FOUND(3001, "Plan Not Found"),
    PLAN_NAME_EXISTED(3002, "Plan Name Existed"),
    INVALID_PLAN_CONFIG(3003, "Invalid Plan Configuration"),
    PLAN_IN_USE(3004, "Plan Is Being Used And Cannot Be Deleted"),
    // --- Staff assignment related ---
    STAFF_NOT_FOUND(4001, "Staff Not Found"),
    STAFF_ALREADY_ASSIGNED(4002, "Staff Already Assigned"),
    STAFF_NOT_IN_STATION(4003, "Staff Not In This Station"),
    // --- Vehicle related ---
    VEHICLE_NOT_FOUND(5001, "Vehicle Not Found"),
    LICENSE_PLATE_EXISTED(5002, "License Plate Already Exists"),
    VEHICLE_NOT_BELONG_TO_DRIVER(5003, "Vehicle Does Not Belong To This Driver"),
    // --- Driver related ---
    DRIVER_NOT_FOUND(6001, "Driver Not Found"),
    // --- Subscription related ---
    SUBSCRIPTION_NOT_FOUND(7001, "Subscription Not Found"),
    SUBSCRIPTION_ALREADY_ACTIVE(7002, "Subscription Already Active"),
    SUBSCRIPTION_NOT_ACTIVE(7003, "Subscription Not Active"),
    // --- Payment Method related ---
    PAYMENT_METHOD_NOT_FOUND(8001, "Payment Method Not Found"),
    PAYMENT_METHOD_REQUIRED(8002, "Payment Method Required For Paid Plan"),
    // --- Charging Session related ---
    SESSION_NOT_FOUND(9001, "Charging Session Not Found"),
    // --- General ---
    UNAUTHORIZED(9002, "Unauthorized Access"),
    // --- Geocoding related ---
    GEOCODING_FAILED(10001, "Failed To Convert Address To Coordinates"),
    INVALID_COORDINATES(10002, "Invalid Coordinates Provided"),
    // --- Charging Point related ---
    CHARGING_POINT_NOT_FOUND(11001, "Charging Point Not Found"),
    // --- Payment related ---
    PAYMENT_ALREADY_EXISTS(12001, "Payment Already Exists For This Session"),
    // --- Incident related ---
    INCIDENT_NOT_FOUND(13001, "Incident Not Found"),
    USER_NOT_EXISTED(14001, "User Not Existed")
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
