package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {

    final JavaMailSender mailSender;

    @Value("${mail.from}")
    String fromEmail;

    final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendChargingStartEmail(ChargingSession session) {
        try {
            User user = session.getDriver().getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for session {}", session.getSessionId());
                return;
            }

            String subject = "⚡ Phiên sạc của bạn đã bắt đầu";
            String htmlContent = buildChargingStartEmailTemplate(session);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent charging start email to {} for session {}", user.getEmail(), session.getSessionId());
        } catch (Exception e) {
            log.error("Failed to send charging start email for session {}: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    @Async
    public void sendChargingCompleteEmail(ChargingSession session) {
        try {
            User user = session.getDriver().getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for session {}", session.getSessionId());
                return;
            }

            String subject = "✅ Phiên sạc của bạn đã hoàn tất";
            String htmlContent = buildChargingCompleteEmailTemplate(session);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent charging complete email to {} for session {}", user.getEmail(), session.getSessionId());
        } catch (Exception e) {
            log.error("Failed to send charging complete email for session {}: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    @Async
    public void sendPaymentConfirmationEmail(Payment payment) {
        try {
            User user = payment.getPayer().getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for payment {}", payment.getPaymentId());
                return;
            }

            String subject = "💳 Thanh toán thành công";
            String htmlContent = buildPaymentConfirmationEmailTemplate(payment);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent payment confirmation email to {} for payment {}", user.getEmail(), payment.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email for payment {}: {}", payment.getPaymentId(), e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String buildChargingStartEmailTemplate(ChargingSession session) {
        String userName = session.getDriver().getUser().getFullName();
        String stationName = session.getChargingPoint().getStation().getName();
        String startTime = session.getStartTime().format(timeFormatter);
        int currentSoc = session.getStartSocPercent();
        int targetSoc = session.getTargetSocPercent() != null ? session.getTargetSocPercent() : 100;

        String bodyContent = String.format(
            "<p>Phiên sạc của bạn tại trạm <strong>%s</strong> đã bắt đầu.</p>" +
            "<ul><li><strong>Thời gian:</strong> %s</li>" +
            "<li><strong>Pin hiện tại:</strong> %d%%</li>" +
            "<li><strong>Mục tiêu:</strong> %d%%</li></ul>",
            stationName, startTime, currentSoc, targetSoc
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }

    private String buildChargingCompleteEmailTemplate(ChargingSession session) {
        String userName = session.getDriver().getUser().getFullName();
        String stationName = session.getChargingPoint().getStation().getName();
        String duration = formatDuration(session.getDurationMin());
        String energy = String.format("%.1f", session.getEnergyKwh());
        int startSoc = session.getStartSocPercent();
        int endSoc = session.getEndSocPercent();
        String cost = currencyFormatter.format(session.getCostTotal());

        String bodyContent = String.format(
            "<p>Phiên sạc của bạn tại trạm <strong>%s</strong> đã hoàn tất.</p>" +
            "<ul><li><strong>Thời gian:</strong> %s</li>" +
            "<li><strong>Năng lượng:</strong> %s kWh</li>" +
            "<li><strong>Pin:</strong> %d%% → %d%%</li>" +
            "<li style='font-size:18px'><strong>Tổng:</strong> %s VNĐ</li></ul>",
            stationName, duration, energy, startSoc, endSoc, cost
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }

    private String buildPaymentConfirmationEmailTemplate(Payment payment) {
        ChargingSession session = payment.getChargingSession();
        String userName = payment.getPayer().getUser().getFullName();
        String sessionId = session.getSessionId().substring(0, 8).toUpperCase();
        String amount = currencyFormatter.format(payment.getAmount());
        String paymentTime = payment.getPaidAt() != null ? payment.getPaidAt().format(timeFormatter) : "N/A";

        String bodyContent = String.format(
            "<p>Thanh toán cho phiên sạc <strong>#%s</strong> đã thành công.</p>" +
            "<ul><li style='font-size:18px'><strong>Số tiền:</strong> %s VNĐ</li>" +
            "<li><strong>Thời gian:</strong> %s</li></ul>",
            sessionId, amount, paymentTime
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }

    private String buildBaseEmailTemplate(String userName, String bodyContent) {
        return String.format(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
            "<body style='font-family:Arial,sans-serif;background:#f9f9f9;margin:0;padding:20px'>" +
            "<div style='max-width:600px;margin:auto;background:#fff;border:1px solid #ddd;border-radius:8px'>" +
            "<div style='background:#15919B;color:#fff;padding:20px;text-align:center'><h1>⚡ EV Charging</h1></div>" +
            "<div style='padding:30px'><p>Chào %s,</p>%s<p>Cảm ơn bạn đã sử dụng dịch vụ.</p>" +
            "<p>Trân trọng,<br>Đội ngũ EV Charging</p></div>" +
            "<div style='background:#f4f4f4;color:#777;padding:20px;text-align:center;font-size:12px'>" +
            "<p>&copy; 2025 EV Charging</p></div></div></body></html>",
            userName, bodyContent
        );
    }

    private String formatDuration(float minutes) {
        int totalMinutes = Math.round(minutes);
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours == 0) return mins + " phút";
        if (mins == 0) return hours + " giờ";
        return hours + " giờ " + mins + " phút";
    }
}

