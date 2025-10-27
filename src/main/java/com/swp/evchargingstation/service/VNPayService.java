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
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public String createVnPayPayment(HttpServletRequest request) {
        // Lấy parameters từ request - GIỐNG CODE MẪU VNPAY
        String sessionId = request.getParameter("sessionId");
        String bankCode = request.getParameter("bankCode");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.error("SessionId parameter is missing or empty");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        log.info("Creating VNPay payment for sessionId: {}, bankCode: {}", sessionId, bankCode);

        // Tìm charging session
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_SESSION_NOT_FOUND));

        // Phiên sạc phải kết thúc mới được thanh toán
        if (session.getEndTime() == null) {
            throw new AppException(ErrorCode.CHARGING_SESSION_NOT_COMPLETED);
        }

        // Lấy số tiền cần thanh toán (nhân 100 theo quy định VNPay)
        long amount = (long) (session.getCostTotal() * 100);

        // Lấy cấu hình VNPay - GIỮ NGUYÊN các giá trị random như code mẫu VNPay
        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();

        // KHÔNG ghi đè vnp_TxnRef - giữ nguyên random theo mẫu VNPay
        // Lưu OrderInfo có sessionId để tiện hiển thị/trace
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan phien sac " + sessionId);

        // Bắt buộc: số tiền, IP, bank code nếu có
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }

        // Debug tham số
        log.info("VNPay params: {}", vnpParamsMap);

        // Build query và hash theo đúng tài liệu VNPay
        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);   // encode keys/values
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);  // encode values only
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        // Tạo/cập nhật payment record: gắn txnReference = vnp_TxnRef random
        String txnRef = vnpParamsMap.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByChargingSession(session).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (payment == null) {
            payment = Payment.builder()
                    .chargingSession(session)
                    .payer(session.getDriver())
                    .amount(session.getCostTotal())
                    .status(PaymentStatus.PENDING)
                    .txnReference(txnRef)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTxnReference(txnRef); // cập nhật txnRef cho lần thanh toán mới
            payment.setUpdatedAt(LocalDateTime.now());
        }
        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request just created the row; reuse it
            log.warn("Payment row already exists for session {}. Reusing existing.", session.getSessionId());
            payment = paymentRepository.findByChargingSession(session)
                    .orElse(payment); // fallback to current reference
        }

        log.info("Created VNPay payment URL for session: {}, amount: {}, txnRef: {}", session.getSessionId(), amount, txnRef);
        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Transactional
    public void handleVNPayCallback(Map<String, String> params) {
        // Verify signature
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        String hashData = VNPayUtil.getPaymentURL(params, false);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        if (!calculatedHash.equals(vnpSecureHash)) {
            log.error("Invalid VNPay signature");
            throw new AppException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }

        // Lấy thông tin giao dịch
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionId = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");
        String payDate = params.get("vnp_PayDate");
        String amountStr = params.get("vnp_Amount");

        // Tìm payment theo txnReference
        Payment payment = paymentRepository.findByTxnReference(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Cập nhật payment status
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionId);
            payment.setPaymentMethod("VNPAY");
            payment.setPaymentDetails("BankCode: " + bankCode + ", CardType: " + cardType);
            payment.setUpdatedAt(LocalDateTime.now());

            // Update paidAt từ vnp_PayDate (yyyyMMddHHmmss)
            if (payDate != null && !payDate.isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                    payment.setPaidAt(LocalDateTime.parse(payDate, formatter));
                } catch (Exception e) {
                    log.error("Error parsing pay date: {}", payDate, e);
                    payment.setPaidAt(LocalDateTime.now());
                }
            } else {
                payment.setPaidAt(LocalDateTime.now());
            }

            // Ghi nhận thời điểm paymentTime theo quy ước
            payment.setPaymentTime(LocalDateTime.now());

            log.info("Payment completed: txnRef={}, transaction={}, amount={}", txnRef, transactionId, amountStr);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDetails("Failed with code: " + responseCode);
            payment.setUpdatedAt(LocalDateTime.now());
            log.warn("Payment failed: txnRef={}, response code={}", txnRef, responseCode);
        }

        paymentRepository.save(payment);
    }
}
