package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StaffPaymentRequest;
import com.swp.evchargingstation.dto.response.*;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.StaffDashboardMapper;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffDashboardService {

    StaffRepository staffRepository;
    StationRepository stationRepository;
    ChargingPointRepository chargingPointRepository;
    ChargingSessionRepository chargingSessionRepository;
    PaymentRepository paymentRepository;
    PaymentMethodRepository paymentMethodRepository;
    StaffDashboardMapper staffDashboardMapper;
    CashPaymentService cashPaymentService;

    /**
     * Lấy thông tin dashboard cho staff đang đăng nhập
     */
    public StaffDashboardResponse getStaffDashboard() {
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        // Lấy thống kê hôm nay
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<ChargingSession> todaySessions = chargingSessionRepository
                .findByStationIdAndDateRange(station.getStationId(), startOfDay, endOfDay);

        int todaySessionsCount = todaySessions.size();
        double todayRevenue = todaySessions.stream()
                .mapToDouble(ChargingSession::getCostTotal)
                .sum();

        // Tính thời gian trung bình
        Double avgDuration = todaySessions.stream()
                .filter(s -> s.getDurationMin() > 0)
                .mapToDouble(ChargingSession::getDurationMin)
                .average()
                .orElse(0.0);

        // Thống kê charging points
        List<ChargingPoint> chargingPoints = chargingPointRepository
                .findByStation_StationId(station.getStationId());

        int totalPoints = chargingPoints.size();
        int availablePoints = (int) chargingPoints.stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.AVAILABLE)
                .count();
        int chargingPointsCount = (int) chargingPoints.stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.CHARGING)
                .count();
        int offlinePoints = (int) chargingPoints.stream()
                .filter(cp -> cp.getStatus() == ChargingPointStatus.OUT_OF_SERVICE ||
                              cp.getStatus() == ChargingPointStatus.MAINTENANCE)
                .count();

        return StaffDashboardResponse.builder()
                .todaySessionsCount(todaySessionsCount)
                .todayRevenue(todayRevenue)
                .averageSessionDuration(avgDuration > 0 ? avgDuration : null)
                .stationId(station.getStationId())
                .stationName(station.getName())
                .stationAddress(station.getAddress())
                .totalChargingPoints(totalPoints)
                .availablePoints(availablePoints)
                .chargingPoints(chargingPointsCount)
                .offlinePoints(offlinePoints)
                .build();
    }

    /**
     * Lấy danh sách charging points của station mà staff quản lý
     */
    public List<StaffChargingPointResponse> getStaffChargingPoints() {
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        List<ChargingPoint> chargingPoints = chargingPointRepository
                .findByStation_StationId(station.getStationId());

        return chargingPoints.stream()
                .map(staffDashboardMapper::toStaffChargingPointResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách giao dịch (sessions) của station
     */
    public List<StaffTransactionResponse> getStaffTransactions() {
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        List<ChargingSession> sessions = chargingSessionRepository
                .findByStationIdOrderByStartTimeDesc(station.getStationId());

        return sessions.stream().map(session -> {
            StaffTransactionResponse response = staffDashboardMapper.toStaffTransactionResponse(session);
            // Set isPaid manually
            response.setPaid(paymentRepository.existsByChargingSession_SessionId(session.getSessionId()));
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * Xử lý thanh toán cho driver (cho phép staff tính tiền trực tiếp)
     */
    @Transactional
    public ApiResponse<String> processPaymentForDriver(StaffPaymentRequest request) {
        throw new com.swp.evchargingstation.exception.AppException(
                com.swp.evchargingstation.exception.ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
    }

    /**
     * Lấy hồ sơ của staff đang đăng nhập
     */
    public StaffProfileResponse getMyProfile() {
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        User user = staff.getUser();
        Station station = staff.getStation();

        return StaffProfileResponse.builder()
                .staffId(staff.getUserId())
                .email(user != null ? user.getEmail() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phone(user != null ? user.getPhone() : null)
                .employeeNo(staff.getEmployeeNo())
                .position(staff.getPosition())
                .stationId(station != null ? station.getStationId() : null)
                .stationName(station != null ? station.getName() : null)
                .stationAddress(station != null ? station.getAddress() : null)
                .build();
    }

    /**
     * Lấy danh sách payments đang chờ xác nhận thanh toán tiền mặt
     */
    public List<PendingPaymentResponse> getPendingCashPayments(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        // Lấy tất cả pending cash payments của station này
        // Query: status = PENDING AND paymentMethod = CASH AND assignedStaff IS NOT NULL
        List<Payment> pendingPayments = paymentRepository.findPendingCashPaymentsByStationId(
                station.getStationId()
        );

        return pendingPayments.stream()
                .map(this::convertToPendingPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Staff xác nhận đã nhận tiền mặt từ driver
     */
    @Transactional
    public ApiResponse<String> confirmCashPayment(String paymentId, String staffId) {
        // Kiểm tra staff có quyền không (payment phải thuộc station của staff)
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Kiểm tra payment thuộc station của staff
        String paymentStationId = payment.getChargingSession().getChargingPoint().getStation().getStationId();
        if (!paymentStationId.equals(staff.getStation().getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Xác nhận thanh toán
        cashPaymentService.confirmCashPayment(paymentId);

        return ApiResponse.<String>builder()
                .message("Đã xác nhận thanh toán tiền mặt thành công")
                .result("COMPLETED")
                .build();
    }

    /**
     * Convert Payment entity sang PendingPaymentResponse
     */
    private PendingPaymentResponse convertToPendingPaymentResponse(Payment payment) {
        ChargingSession session = payment.getChargingSession();
        Driver driver = session.getDriver();
        User driverUser = driver.getUser();
        Vehicle vehicle = session.getVehicle();
        ChargingPoint point = session.getChargingPoint();
        Station station = point.getStation();

        return PendingPaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .sessionId(session.getSessionId())
                .driverName(driverUser != null ? driverUser.getFullName() : "N/A")
                .driverPhone(driverUser != null ? driverUser.getPhone() : "N/A")
                .licensePlate(vehicle != null ? vehicle.getLicensePlate() : "N/A")
                .sessionStartTime(session.getStartTime())
                .sessionEndTime(session.getEndTime())
                .durationMin(session.getDurationMin())
                .energyKwh(session.getEnergyKwh())
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus().name())
                .requestedAt(payment.getCreatedAt())
                .stationName(station != null ? station.getName() : "N/A")
                .chargingPointName(point.getName() != null ? point.getName() : point.getPointId())
                .build();
    }

    // Helper methods
    private String getCurrentStaffUserId() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            String userId = jwt.getClaim("userId");
            if (userId != null) return userId;
        }
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
