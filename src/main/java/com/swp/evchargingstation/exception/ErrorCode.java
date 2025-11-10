package com.swp.evchargingstation.exception;

public enum ErrorCode {
    UNAUTHORIZED_EXCEPTION(3979, "Unauthorized Exception"),
    USER_EXISTED(1001, "User Existed"),
    PASSWORD_NOT_MATCH(1002, "Password Not Match"),
    EMAIL_EXISTED(1003, "Email Existed"),
    USER_NOT_FOUND(1004, "User Not Found"),
    UNAUTHENTICATED(1005, "Unauthenticated"),
    VALIDATION_FAILED(1006, "Validation Failed"), // generic validation error
    STATION_NOT_FOUND(2001, "Station Not Found"), // added for station module
    STATION_NAME_REQUIRED(2002, "Station Name Is Required"),
    STATION_ADDRESS_REQUIRED(2003, "Station Address Is Required"),
    STATION_CHARGING_POINTS_REQUIRED(2004, "Number Of Charging Points Is Required"),
    STATION_CHARGING_POINTS_MIN(2005, "Number Of Charging Points Must Be Greater Than 0"),
    STATION_POWER_OUTPUT_REQUIRED(2006, "Power Output Is Required"),
    STATION_STATUS_REQUIRED(2007, "Station Status Is Required"),
    LATITUDE_INVALID(2008, "Latitude Must Be Between -90 And 90"),
    LONGITUDE_INVALID(2009, "Longitude Must Be Between -180 And 180"),
    // --- Plan related ---
    PLAN_NOT_FOUND(6001, "Plan Not Found"),
    PLAN_NAME_EXISTED(6002, "Plan Name Already Exists"),
    INVALID_PLAN_CONFIG(6003, "Invalid Plan Configuration"),
    PLAN_IN_USE(3004, "Plan Is Being Used And Cannot Be Deleted"),
    // --- Staff assignment related ---
    STAFF_NOT_FOUND(4001, "Staff Not Found"),
    STAFF_ALREADY_ASSIGNED(4002, "Staff Already Assigned"),
    STAFF_NOT_IN_STATION(4003, "Staff Not In This Station"),
    // --- Vehicle related ---
    VEHICLE_NOT_FOUND(5001, "Vehicle Not Found"),
    LICENSE_PLATE_EXISTED(5002, "License Plate Already Exists"),
    VEHICLE_NOT_BELONG_TO_DRIVER(5003, "Vehicle Does Not Belong To This Driver"),
    INVALID_VEHICLE_MODEL_FOR_BRAND(5004, "Vehicle Model Does Not Match Selected Brand"),
    // --- Driver related ---
    DRIVER_NOT_FOUND(6001, "Driver Not Found"),
    // --- Payment related ---
    PAYMENT_NOT_FOUND(8001, "Payment Not Found"),
    PAYMENT_METHOD_REQUIRED(8002, "Payment Method Required For Paid Plan"),
    PAYMENT_METHOD_NOT_FOUND(8003, "Payment Method Not Found"),
    // --- Charging Session related ---
    SESSION_NOT_FOUND(9001, "Charging Session Not Found"),
    CHARGING_POINT_ID_REQUIRED(9002, "Charging Point ID Is Required"),
    VEHICLE_ID_REQUIRED(9003, "Vehicle ID Is Required"),
    TARGET_SOC_INVALID(9004, "Target SOC Must Be Between Current SOC And 100"),
    // --- General ---
    UNAUTHORIZED(9002, "Unauthorized Access"),
    // --- Geocoding related ---
    GEOCODING_FAILED(10001, "Failed To Convert Address To Coordinates"),
    INVALID_COORDINATES(10002, "Invalid Coordinates Provided"),
    // --- Charging Point related ---
    CHARGING_POINT_NOT_FOUND(11001, "Charging Point Not Found"),
    CHARGING_POINT_IN_USE(11002, "Charging Point Is In Use And Cannot Be Deleted"),
    // --- Payment related ---
    PAYMENT_ALREADY_EXISTS(12001, "Payment Already Exists For This Session"),
    // --- Incident related ---
    INCIDENT_NOT_FOUND(13001, "Incident Not Found"),
    USER_NOT_EXISTED(14001, "User Not Existed"),
    // --- Charging Simulation related ---
    CHARGING_POINT_NOT_AVAILABLE(15001, "Charging Point Not Available"),
    INVALID_SOC_RANGE(15002, "Start SOC Must Be Less Than Target SOC"),
    CHARGING_SESSION_NOT_FOUND(15003, "Charging Session Not Found"),
    CHARGING_SESSION_NOT_ACTIVE(15004, "Charging Session Is Not Active"),

    // Payment related
    PAYMENT_ALREADY_COMPLETED(16002, "Payment Already Completed"),

    // ZaloPay errors (17xxx)
    ZALOPAY_API_ERROR(17000, "ZaloPay API error"),
    ZALOPAY_INVALID_MAC(17001, "Invalid ZaloPay MAC signature"),
    ZALOPAY_CALLBACK_ERROR(17002, "ZaloPay callback processing error"),

    // Active Session related
    NO_ACTIVE_SESSION(17001, "No Active Charging Session Found"),

    // Cash Payment related
    SESSION_NOT_COMPLETED(18001, "Charging Session Not Completed"),
    CASH_PAYMENT_REQUEST_ALREADY_EXISTS(18002, "Cash Payment Request Already Exists"),
    STATION_NO_STAFF(18003, "Station Has No Assigned Staff"),
    STAFF_NO_MANAGED_STATION(18004, "Staff Does Not Manage Any Station"),
    CASH_PAYMENT_REQUEST_NOT_FOUND(18005, "Cash Payment Request Not Found"),
    CASH_PAYMENT_REQUEST_ALREADY_PROCESSED(18006, "Cash Payment Request Already Processed"),
    STAFF_NOT_AUTHORIZED_FOR_STATION(18007, "Staff Not Authorized To Process Payments For This Station"),

    // Wallet related (19xxx)
    WALLET_NOT_FOUND(19001, "Wallet Not Found"),
    INSUFFICIENT_FUNDS(19002, "Insufficient Funds In Wallet"),
    WALLET_TRANSACTION_NOT_FOUND(19003, "Wallet Transaction Not Found"),
    INVALID_TOPUP_AMOUNT(19004, "Invalid Top-up Amount"),
    WALLET_ALREADY_EXISTS(19005, "Wallet Already Exists For This User"),
    TRANSACTION_FAILED(19006, "Transaction Failed"),

    // Booking related (20xxx)
    VEHICLE_NOT_MATCH_BOOKING(20001, "Vehicle Does Not Match Booking"),
    CHARGING_POINT_RESERVED(20002, "Charging Point Is Reserved");

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
