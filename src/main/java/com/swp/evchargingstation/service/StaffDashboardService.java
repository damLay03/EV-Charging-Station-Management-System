package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.IncidentCreationRequest;
import com.swp.evchargingstation.dto.request.IncidentUpdateRequest;
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
    IncidentRepository incidentRepository;
    UserRepository userRepository;
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
                .mapToInt(ChargingSession::getDurationMin)
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
                .filter(cp -> cp.getStatus() == ChargingPointStatus.OCCUPIED)
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
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        ChargingSession session = chargingSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        // Kiểm tra session thuộc station của staff
        if (!session.getChargingPoint().getStation().getStationId()
                .equals(staff.getStation().getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra đã thanh toán chưa
        if (paymentRepository.existsByChargingSession_SessionId(session.getSessionId())) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        Payment payment = Payment.builder()
                .payer(session.getDriver())
                .amount(request.getAmount())
                .method(paymentMethod)
                .paymentTime(LocalDateTime.now())
                .status(PaymentStatus.COMPLETED)
                .txnReference("STAFF_" + System.currentTimeMillis())
                .chargingSession(session)
                .build();

        paymentRepository.save(payment);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Thanh toán thành công")
                .result("Payment ID: " + payment.getPaymentId())
                .build();
    }

    /**
     * Tạo báo cáo sự cố
     */
    @Transactional
    public ApiResponse<IncidentResponse> createIncident(IncidentCreationRequest request) {
        String staffUserId = getCurrentStaffUserId();
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // Kiểm tra staff có quản lý station này không
        if (!station.getStationId().equals(staff.getStation().getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ChargingPoint chargingPoint = null;
        if (request.getChargingPointId() != null) {
            chargingPoint = chargingPointRepository.findById(request.getChargingPointId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        }

        Incident incident = Incident.builder()
                .reporter(staffUser)
                .station(station)
                .chargingPoint(chargingPoint)
                .reportedAt(LocalDateTime.now())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status("REPORTED")
                .assignedStaff(staff)
                .build();

        incident = incidentRepository.save(incident);

        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .message("Báo cáo sự cố thành công")
                .result(staffDashboardMapper.toIncidentResponse(incident))
                .build();
    }

    /**
     * Lấy danh sách sự cố của station
     * - ADMIN: xem tất cả incidents của tất cả stations
     * - STAFF: chỉ xem incidents của station mình quản lý
     */
    public List<IncidentResponse> getStaffIncidents() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check if user is ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        List<Incident> incidents;

        if (isAdmin) {
            // ADMIN: lấy tất cả incidents
            incidents = incidentRepository.findAllByOrderByReportedAtDesc();
        } else {
            // STAFF: chỉ lấy incidents của station mình quản lý
            Staff staff = staffRepository.findByIdWithStation(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

            Station station = staff.getStation();
            if (station == null) {
                throw new AppException(ErrorCode.STATION_NOT_FOUND);
            }

            incidents = incidentRepository.findByStationIdOrderByReportedAtDesc(station.getStationId());
        }

        return incidents.stream()
                .map(staffDashboardMapper::toIncidentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật trạng thái sự cố
     */
    @Transactional
    public ApiResponse<IncidentResponse> updateIncident(String incidentId, IncidentUpdateRequest request) {
        String staffUserId = getCurrentStaffUserId();
        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        // Kiểm tra incident thuộc station của staff
        if (!incident.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        incident.setStatus(request.getStatus());

        if ("RESOLVED".equals(request.getStatus()) || "CLOSED".equals(request.getStatus())) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        incident = incidentRepository.save(incident);

        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .message("Cập nhật sự cố thành công")
                .result(staffDashboardMapper.toIncidentResponse(incident))
                .build();
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

        // Lấy tất cả payments có status = PENDING_CASH của station này
        List<Payment> pendingPayments = paymentRepository.findByStationIdAndStatus(
                station.getStationId(),
                PaymentStatus.PENDING_CASH
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
