package com.swp.evchargingstation.exception;

public enum ErrorCode {
    UNAUTHORIZED_EXCEPTION(3979, "Không có quyền truy cập"),
    USER_EXISTED(1001, "Người dùng đã tồn tại"),
    PASSWORD_NOT_MATCH(1002, "Mật khẩu không khớp"),
    EMAIL_EXISTED(1003, "Email đã tồn tại"),
    USER_NOT_FOUND(1004, "Không tìm thấy người dùng"),
    UNAUTHENTICATED(1005, "Chưa xác thực"),
    VALIDATION_FAILED(1006, "Xác thực thất bại"), // generic validation error
    STATION_NOT_FOUND(2001, "Không tìm thấy trạm sạc"), // added for station module
    STATION_NAME_REQUIRED(2002, "Tên trạm sạc là bắt buộc"),
    STATION_ADDRESS_REQUIRED(2003, "Địa chỉ trạm sạc là bắt buộc"),
    STATION_CHARGING_POINTS_REQUIRED(2004, "Số lượng trụ sạc là bắt buộc"),
    STATION_CHARGING_POINTS_MIN(2005, "Số lượng trụ sạc phải lớn hơn 0"),
    STATION_POWER_OUTPUT_REQUIRED(2006, "Công suất đầu ra là bắt buộc"),
    STATION_STATUS_REQUIRED(2007, "Trạng thái trạm sạc là bắt buộc"),
    LATITUDE_INVALID(2008, "Vĩ độ phải nằm trong khoảng -90 đến 90"),
    LONGITUDE_INVALID(2009, "Kinh độ phải nằm trong khoảng -180 đến 180"),
    // --- Plan related ---
    PLAN_NOT_FOUND(6001, "Không tìm thấy gói dịch vụ"),
    PLAN_NAME_EXISTED(6002, "Tên gói dịch vụ đã tồn tại"),
    INVALID_PLAN_CONFIG(6003, "Cấu hình gói dịch vụ không hợp lệ"),
    PLAN_IN_USE(3004, "Gói dịch vụ đang được sử dụng và không thể xóa"),
    // --- Staff assignment related ---
    STAFF_NOT_FOUND(4001, "Không tìm thấy nhân viên"),
    STAFF_ALREADY_ASSIGNED(4002, "Nhân viên đã được phân công"),
    STAFF_NOT_IN_STATION(4003, "Nhân viên không thuộc trạm sạc này"),
    // Vehicle related ---
    VEHICLE_NOT_FOUND(5001, "Không tìm thấy phương tiện"),
    LICENSE_PLATE_EXISTED(5002, "Biển số xe đã tồn tại"),
    VEHICLE_NOT_BELONG_TO_DRIVER(5003, "Phương tiện không thuộc về tài xế này"),
    INVALID_VEHICLE_MODEL_FOR_BRAND(5004, "Mẫu xe không khớp với hãng đã chọn"),
    // --- Driver related ---
    DRIVER_NOT_FOUND(6001, "Không tìm thấy tài xế"),
    // --- Payment related ---
    PAYMENT_NOT_FOUND(8001, "Không tìm thấy thanh toán"),
    PAYMENT_METHOD_REQUIRED(8002, "Yêu cầu phương thức thanh toán cho gói trả phí"),
    PAYMENT_METHOD_NOT_FOUND(8003, "Không tìm thấy phương thức thanh toán"),
    // --- Charging Session related ---
    SESSION_NOT_FOUND(9001, "Không tìm thấy phiên sạc"),
    CHARGING_POINT_ID_REQUIRED(9002, "Mã trụ sạc là bắt buộc"),
    VEHICLE_ID_REQUIRED(9003, "Mã phương tiện là bắt buộc"),
    TARGET_SOC_INVALID(9004, "Mức SOC mục tiêu phải nằm giữa SOC hiện tại và 100"),
    // --- General ---
    UNAUTHORIZED(9002, "Không có quyền truy cập"),
    // --- Geocoding related ---
    GEOCODING_FAILED(10001, "Không thể chuyển đổi địa chỉ thành tọa độ"),
    INVALID_COORDINATES(10002, "Tọa độ không hợp lệ"),
    // --- Charging Point related ---
    CHARGING_POINT_NOT_FOUND(11001, "Không tìm thấy trụ sạc"),
    CHARGING_POINT_IN_USE(11002, "Trụ sạc đang được sử dụng và không thể xóa"),
    // --- Payment related ---
    PAYMENT_ALREADY_EXISTS(12001, "Thanh toán đã tồn tại cho phiên sạc này"),
    // --- Incident related ---
    INCIDENT_NOT_FOUND(13001, "Không tìm thấy sự cố"),
    USER_NOT_EXISTED(14001, "Người dùng không tồn tại"),
    // --- Charging Simulation related ---
    CHARGING_POINT_NOT_AVAILABLE(15001, "Trụ sạc không khả dụng"),
    INVALID_SOC_RANGE(15002, "SOC bắt đầu phải nhỏ hơn SOC mục tiêu"),
    CHARGING_SESSION_NOT_FOUND(15003, "Không tìm thấy phiên sạc"),
    CHARGING_SESSION_NOT_ACTIVE(15004, "Phiên sạc không hoạt động"),

    // Payment related
    PAYMENT_ALREADY_COMPLETED(16002, "Thanh toán đã hoàn tất"),
    PAYMENT_METHOD_NOT_ALLOWED(16003, "Chỉ chấp nhận thanh toán bằng ví điện tử cho phiên sạc"),

    // ZaloPay errors (17xxx)
    ZALOPAY_API_ERROR(17000, "Lỗi API ZaloPay"),
    ZALOPAY_INVALID_MAC(17001, "Chữ ký MAC ZaloPay không hợp lệ"),
    ZALOPAY_CALLBACK_ERROR(17002, "Lỗi xử lý callback ZaloPay"),

    // Active Session related
    NO_ACTIVE_SESSION(17001, "Không tìm thấy phiên sạc đang hoạt động"),

    // Cash Payment related
    SESSION_NOT_COMPLETED(18001, "Phiên sạc chưa hoàn thành"),
    CASH_PAYMENT_REQUEST_ALREADY_EXISTS(18002, "Yêu cầu thanh toán tiền mặt đã tồn tại"),
    STATION_NO_STAFF(18003, "Trạm sạc không có nhân viên được phân công"),
    STAFF_NO_MANAGED_STATION(18004, "Nhân viên không quản lý trạm sạc nào"),
    CASH_PAYMENT_REQUEST_NOT_FOUND(18005, "Không tìm thấy yêu cầu thanh toán tiền mặt"),
    CASH_PAYMENT_REQUEST_ALREADY_PROCESSED(18006, "Yêu cầu thanh toán tiền mặt đã được xử lý"),
    STAFF_NOT_AUTHORIZED_FOR_STATION(18007, "Nhân viên không có quyền xử lý thanh toán cho trạm sạc này"),

    // Wallet related (19xxx)
    WALLET_NOT_FOUND(19001, "Không tìm thấy ví điện tử"),
    INSUFFICIENT_FUNDS(19002, "Số dư ví không đủ"),
    WALLET_TRANSACTION_NOT_FOUND(19003, "Không tìm thấy giao dịch ví"),
    INVALID_TOPUP_AMOUNT(19004, "Số tiền nạp không hợp lệ"),
    WALLET_ALREADY_EXISTS(19005, "Ví điện tử đã tồn tại cho người dùng này"),
    TRANSACTION_FAILED(19006, "Giao dịch thất bại"),

    // Booking related (20xxx)
    BOOKING_NOT_FOUND(20001, "Không tìm thấy đặt chỗ"),
    VEHICLE_NOT_MATCH_BOOKING(20002, "Phương tiện không khớp với đặt chỗ"),
    CHARGING_POINT_RESERVED(20003, "Trụ sạc đã được đặt trước"),
    FORBIDDEN(20004, "Truy cập bị cấm"),
    CHARGING_POINT_BUSY(20005, "Trụ sạc hiện đang bận"),
    INSUFFICIENT_TIME_BETWEEN_BOOKINGS(20006, "Không đủ thời gian giữa các booking"),

    // Plan subscription related (21xxx)
    CANNOT_DOWNGRADE_TO_FLEXIBLE(21001, "Không thể hạ cấp xuống gói thấp hơn khi đang có gói đăng ký hoạt động"),
    PLAN_STILL_ACTIVE(21002, "Gói hiện tại vẫn đang hoạt động"),

    // Vehicle Approval related (22xxx)
    VEHICLE_ALREADY_PROCESSED(22001, "Đăng ký phương tiện đã được xử lý"),
    VEHICLE_NOT_APPROVED(22002, "Phương tiện chưa được phê duyệt để sạc"),
    ADMIN_NOT_FOUND(22003, "Không tìm thấy quản trị viên"),

    // File Upload related (23xxx)
    INVALID_FILE(23001, "Tệp không hợp lệ"),
    INVALID_FILE_TYPE(23002, "Loại tệp không hợp lệ. Chỉ chấp nhận hình ảnh"),
    FILE_TOO_LARGE(23003, "Kích thước tệp vượt quá giới hạn 5MB"),
    UPLOAD_FAILED(23004, "Tải tệp lên thất bại");

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
