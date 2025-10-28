package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.CashPaymentRequestResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.ChargingSessionStatus;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CashPaymentService {

    PaymentRepository paymentRepository;
    ChargingSessionRepository chargingSessionRepository;
    StaffRepository staffRepository;

    /**
     * Driver yêu cầu thanh toán bằng tiền mặt cho một session đã hoàn thành
     */
    @Transactional
    public CashPaymentRequestResponse requestCashPayment(String sessionId) {
        String userId = getUserIdFromToken();

        log.info("Driver {} requesting cash payment for session {}", userId, sessionId);

        // Kiểm tra session tồn tại
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        // Kiểm tra session thuộc về driver này
        if (!session.getDriver().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra session đã hoàn thành chưa
        if (session.getStatus() != ChargingSessionStatus.COMPLETED) {
            throw new AppException(ErrorCode.SESSION_NOT_COMPLETED);
        }

        // Lấy Payment record (phải đã tồn tại với status UNPAID khi session COMPLETED)
        Payment payment = paymentRepository.findByChargingSession(session)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Kiểm tra payment status
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // Nếu đã có cash payment request rồi (paymentMethod = CASH và assignedStaff != null)
        if ("CASH".equals(payment.getPaymentMethod()) && payment.getAssignedStaff() != null) {
            throw new AppException(ErrorCode.CASH_PAYMENT_REQUEST_ALREADY_EXISTS);
        }

        // Lấy thông tin station và staff quản lý
        Station station = session.getChargingPoint().getStation();
        Staff assignedStaff = station.getStaff();

        if (assignedStaff == null) {
            throw new AppException(ErrorCode.STATION_NO_STAFF);
        }

        // Update payment: UNPAID → PENDING với paymentMethod = CASH
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("CASH");
        payment.setAssignedStaff(assignedStaff);
        payment.setUpdatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        log.info("Cash payment request created for session {}, payment updated from UNPAID to PENDING", sessionId);

        return convertToResponse(payment);
    }

    /**
     * Staff lấy danh sách các yêu cầu thanh toán đang chờ tại trạm của mình
     */
    public List<CashPaymentRequestResponse> getPendingCashPaymentRequests() {
        String userId = getUserIdFromToken();

        log.info("Staff {} getting pending cash payment requests", userId);

        // Lấy thông tin staff
        Staff staff = staffRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Lấy station mà staff quản lý
        Station managedStation = staff.getManagedStation();

        if (managedStation == null) {
            throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
        }

        // Lấy danh sách pending cash payments tại trạm này
        // Query: status = PENDING AND paymentMethod = CASH AND assignedStaff IS NOT NULL
        List<Payment> pendingPayments = paymentRepository
                .findPendingCashPaymentsByStationId(managedStation.getStationId());

        return pendingPayments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Staff xác nhận driver đã thanh toán tiền mặt
     */
    @Transactional
    public CashPaymentRequestResponse confirmCashPayment(String paymentId) {
        String userId = getUserIdFromToken();

        log.info("Staff {} confirming cash payment {}", userId, paymentId);

        // Lấy thông tin staff
        Staff staff = staffRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Lấy station mà staff quản lý
        Station managedStation = staff.getManagedStation();

        if (managedStation == null) {
            log.error("Staff {} does not manage any station", userId);
            throw new AppException(ErrorCode.STAFF_NO_MANAGED_STATION);
        }

        // Lấy payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment before update - ID: {}, Status: {}, Method: {}",
                payment.getPaymentId(), payment.getStatus(), payment.getPaymentMethod());

        // Kiểm tra payment có thuộc trạm của staff này không
        Station paymentStation = payment.getChargingSession().getChargingPoint().getStation();

        if (!managedStation.getStationId().equals(paymentStation.getStationId())) {
            log.error("Staff {} (manages station {}) attempted to confirm payment {} from station {}",
                    userId, managedStation.getStationId(), paymentId, paymentStation.getStationId());
            throw new AppException(ErrorCode.STAFF_NOT_AUTHORIZED_FOR_STATION);
        }

        // Kiểm tra đây có phải cash payment request đang chờ không
        if (payment.getStatus() != PaymentStatus.PENDING ||
                !"CASH".equals(payment.getPaymentMethod()) ||
                payment.getAssignedStaff() == null) {
            log.error("Invalid payment state - Status: {}, Method: {}, AssignedStaff: {}",
                    payment.getStatus(), payment.getPaymentMethod(), payment.getAssignedStaff());
            throw new AppException(ErrorCode.CASH_PAYMENT_REQUEST_ALREADY_PROCESSED);
        }

        // Cập nhật payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setConfirmedByStaff(staff);
        payment.setConfirmedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        paymentRepository.flush(); // Force flush to database immediately

        log.info("Payment after update - ID: {}, Status: {}, ConfirmedAt: {}, PaidAt: {}",
                savedPayment.getPaymentId(), savedPayment.getStatus(),
                savedPayment.getConfirmedAt(), savedPayment.getPaidAt());

        return convertToResponse(savedPayment);
    }

    /**
     * Chuyển đổi Payment entity sang CashPaymentRequestResponse DTO
     */
    private CashPaymentRequestResponse convertToResponse(Payment payment) {
        ChargingSession session = payment.getChargingSession();
        Driver driver = payment.getPayer();
        User driverUser = driver.getUser();
        Vehicle vehicle = session.getVehicle();
        Station station = session.getChargingPoint().getStation();

        String confirmedByStaffName = null;
        if (payment.getConfirmedByStaff() != null) {
            User staffUser = payment.getConfirmedByStaff().getUser();
            confirmedByStaffName = staffUser != null ? staffUser.getFullName() : null;
        }

        // Map PaymentStatus sang CashPaymentRequestStatus để frontend dễ hiểu
        String status = mapPaymentStatusToCashStatus(payment.getStatus());

        return CashPaymentRequestResponse.builder()
                .requestId(payment.getPaymentId()) // Sử dụng paymentId làm requestId
                .paymentId(payment.getPaymentId())
                .sessionId(session.getSessionId())
                .driverId(driver.getUserId())
                .driverName(driverUser != null ? driverUser.getFullName() : "")
                .driverPhone(driverUser != null ? driverUser.getPhone() : "")
                .stationName(station.getName())
                .chargingPointName(session.getChargingPoint().getName())
                .sessionStartTime(session.getStartTime())
                .sessionEndTime(session.getEndTime())
                .energyKwh(session.getEnergyKwh())
                .amount(payment.getAmount())
                .status(com.swp.evchargingstation.enums.CashPaymentRequestStatus.valueOf(status))
                .createdAt(payment.getCreatedAt())
                .confirmedAt(payment.getConfirmedAt())
                .confirmedByStaffName(confirmedByStaffName)
                .vehicleModel(vehicle != null && vehicle.getModel() != null ? vehicle.getModel().getModelName() : "")
                .licensePlate(vehicle != null ? vehicle.getLicensePlate() : "")
                .build();
    }

    /**
     * Map PaymentStatus sang CashPaymentRequestStatus
     */
    private String mapPaymentStatusToCashStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case UNPAID -> "PENDING";  // UNPAID cũng hiển thị là PENDING cho frontend
            case PENDING -> "PENDING";
            case COMPLETED -> "CONFIRMED";
            case CANCELLED -> "CANCELLED";
            default -> "PENDING";
        };
    }

    /**
     * Lấy userId từ JWT token
     */
    private String getUserIdFromToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userId;
    }
}
