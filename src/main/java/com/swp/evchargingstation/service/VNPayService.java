package com.swp.evchargingstation.service;

import com.swp.evchargingstation.configuration.payment.VNPAYConfig;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.enums.PaymentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.PaymentRepository;
import com.swp.evchargingstation.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPAYConfig vnPayConfig;
    private final ChargingSessionRepository chargingSessionRepository;
    private final PaymentRepository paymentRepository;

    public String createVnPayPayment(HttpServletRequest request) {
        // Lấy parameters từ request - GIỐNG CODE MẪU VNPAY
        String sessionId = request.getParameter("sessionId");
        String bankCode = request.getParameter("bankCode");

        // Validate sessionId không được null hoặc rỗng
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.error("SessionId parameter is missing or empty");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        log.info("Creating VNPay payment for sessionId: {}, bankCode: {}", sessionId, bankCode);

        // Tìm charging session
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Kiểm tra session đã hoàn thành chưa
        if (session.getEndTime() == null) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_COMPLETED);
        }

        // Kiểm tra đã có payment chưa
        Payment existingPayment = paymentRepository.findByChargingSession(session)
                .orElse(null);

        if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // Lấy số tiền cần thanh toán
        long amount = (long) (session.getCostTotal() * 100); // VNPay yêu cầu nhân 100

        // Lấy cấu hình VNPay - GIỮ NGUYÊN các giá trị random như code mẫu VNPay
        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();

        // GHI ĐÈ vnp_TxnRef bằng sessionId (theo tài liệu VNPay: mã giao dịch unique của merchant)
        vnpParamsMap.put("vnp_TxnRef", sessionId);

        // Cập nhật vnp_OrderInfo với thông tin session
        vnpParamsMap.put("vnp_OrderInfo", "ThanhToanPhienSac" + sessionId);

        // Chỉ thêm các thông tin bắt buộc - GIỐNG CODE MẪU VNPAY
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // Thêm bank code nếu có
        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }

        // Log params để debug
        log.info("VNPay params: {}", vnpParamsMap);

        // Build query URL và hash data - THEO ĐÚNG TÀI LIỆU VNPAY
        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);  // CÓ encode
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false); // KHÔNG encode

        log.info("Hash data string: {}", hashData);

        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);

        log.info("Secure hash: {}", vnpSecureHash);

        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        // Tạo hoặc cập nhật payment record
        if (existingPayment == null) {
            Payment payment = Payment.builder()
                    .chargingSession(session)
                    .amount(session.getCostTotal())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);
        } else {
            existingPayment.setStatus(PaymentStatus.PENDING);
            existingPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(existingPayment);
        }

        log.info("Created VNPay payment URL for session: {}, amount: {}", session.getSessionId(), amount);

        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Transactional
    public void handleVNPayCallback(Map<String, String> params) {
        // Verify signature
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        // Build hash data - GIỐNG CODE MẪU VNPAY
        String hashData = VNPayUtil.getPaymentURL(params, false);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);

        if (!calculatedHash.equals(vnpSecureHash)) {
            log.error("Invalid VNPay signature");
            throw new AppException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }

        // Lấy thông tin giao dịch
        String responseCode = params.get("vnp_ResponseCode");
        String sessionId = params.get("vnp_TxnRef");
        String transactionId = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");
        String payDate = params.get("vnp_PayDate");

        // Tìm charging session
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Tìm payment
        Payment payment = paymentRepository.findByChargingSession(session)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Cập nhật payment status
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionId);
            payment.setPaymentMethod("VNPAY");
            payment.setPaymentDetails("BankCode: " + bankCode + ", CardType: " + cardType);
            payment.setUpdatedAt(LocalDateTime.now());

            // Parse payDate (format: yyyyMMddHHmmss)
            if (payDate != null && !payDate.isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                    payment.setPaidAt(LocalDateTime.parse(payDate, formatter));
                } catch (Exception e) {
                    log.error("Error parsing pay date: {}", payDate, e);
                    payment.setPaidAt(LocalDateTime.now());
                }
            }

            log.info("Payment completed for session: {}, transaction: {}", sessionId, transactionId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDetails("Failed with code: " + responseCode);
            payment.setUpdatedAt(LocalDateTime.now());

            log.warn("Payment failed for session: {}, response code: {}", sessionId, responseCode);
        }

        paymentRepository.save(payment);
    }
}
