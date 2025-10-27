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

        // Kiểm tra đã có payment chưa
        Payment existingPayment = paymentRepository.findByChargingSession(session).orElse(null);

        if (existingPayment != null) {
            // Nếu đã thanh toán rồi
            if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
            }

            // Nếu đã có cash payment request rồi (status = PENDING_CASH)
            if (existingPayment.getStatus() == PaymentStatus.PENDING_CASH) {
                throw new AppException(ErrorCode.CASH_PAYMENT_REQUEST_ALREADY_EXISTS);
            }
        }

        // Lấy thông tin station và staff quản lý
        Station station = session.getChargingPoint().getStation();
        Staff assignedStaff = station.getStaff();

        if (assignedStaff == null) {
            throw new AppException(ErrorCode.STATION_NO_STAFF);
        }

        // Tạo hoặc update payment
        Payment payment;
        if (existingPayment == null) {
            payment = Payment.builder()
                    .payer(session.getDriver())
                    .amount(session.getCostTotal())
                    .paymentMethod("CASH")
                    .status(PaymentStatus.PENDING_CASH)
                    .chargingSession(session)
                    .assignedStaff(assignedStaff)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            existingPayment.setStatus(PaymentStatus.PENDING_CASH);
            existingPayment.setPaymentMethod("CASH");
            existingPayment.setAssignedStaff(assignedStaff);
            existingPayment.setUpdatedAt(LocalDateTime.now());
            payment = existingPayment;
        }

        payment = paymentRepository.save(payment);

        log.info("Cash payment request created for session {}", sessionId);

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
        List<Payment> pendingPayments = paymentRepository
                .findByStationIdAndStatus(managedStation.getStationId(), PaymentStatus.PENDING_CASH);

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

        // Lấy payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Kiểm tra payment có thuộc trạm của staff này không
        Station managedStation = staff.getManagedStation();
        Station paymentStation = payment.getChargingSession().getChargingPoint().getStation();

        if (managedStation == null || !managedStation.getStationId().equals(paymentStation.getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra status
        if (payment.getStatus() != PaymentStatus.PENDING_CASH) {
            throw new AppException(ErrorCode.CASH_PAYMENT_REQUEST_ALREADY_PROCESSED);
        }

        // Cập nhật payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setConfirmedByStaff(staff);
        payment.setConfirmedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        log.info("Cash payment {} confirmed by staff {}", paymentId, userId);

        return convertToResponse(payment);
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
            case PENDING_CASH -> "PENDING";
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
