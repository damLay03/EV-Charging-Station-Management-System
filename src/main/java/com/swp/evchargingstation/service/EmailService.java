package com.swp.evchargingstation.service;

import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.entity.Plan;
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
        try {
            String userName = "Quý khách";
            String stationName = "Trạm sạc";

            // Safe access to lazy-loaded entities
            if (session.getDriver() != null && session.getDriver().getUser() != null) {
                try {
                    userName = session.getDriver().getUser().getFullName();
                } catch (Exception e) {
                    log.warn("Could not load driver user name: {}", e.getMessage());
                }
            }

            if (session.getChargingPoint() != null && session.getChargingPoint().getStation() != null) {
                try {
                    stationName = session.getChargingPoint().getStation().getName();
                } catch (Exception e) {
                    log.warn("Could not load station name: {}", e.getMessage());
                }
            }

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
        } catch (Exception e) {
            log.error("Error building charging start email template: {}", e.getMessage(), e);
            return buildFallbackEmailTemplate();
        }
    }

    private String buildChargingCompleteEmailTemplate(ChargingSession session) {
        try {
            String userName = "Quý khách";
            String stationName = "Trạm sạc";

            // Safe access to lazy-loaded entities
            if (session.getDriver() != null && session.getDriver().getUser() != null) {
                try {
                    userName = session.getDriver().getUser().getFullName();
                } catch (Exception e) {
                    log.warn("Could not load driver user name: {}", e.getMessage());
                }
            }

            if (session.getChargingPoint() != null && session.getChargingPoint().getStation() != null) {
                try {
                    stationName = session.getChargingPoint().getStation().getName();
                } catch (Exception e) {
                    log.warn("Could not load station name: {}", e.getMessage());
                }
            }

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
        } catch (Exception e) {
            log.error("Error building charging complete email template: {}", e.getMessage(), e);
            return buildFallbackEmailTemplate();
        }
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

    private String buildFallbackEmailTemplate() {
        return buildBaseEmailTemplate(
            "Quý khách",
            "<p>Phiên sạc của bạn đã được xử lý.</p>" +
            "<p>Vui lòng kiểm tra chi tiết trong ứng dụng.</p>"
        );
    }

    // ==================== PLAN SUBSCRIPTION EMAILS ====================

    @Async
    public void sendPlanSubscriptionSuccessEmail(Driver driver, Plan plan, double fee) {
        try {
            User user = driver.getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for driver {}", driver.getUserId());
                return;
            }

            String subject = "🎉 Đăng ký gói cước thành công";
            String htmlContent = buildPlanSubscriptionSuccessEmailTemplate(user, plan, fee);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent plan subscription success email to {} for plan {}", user.getEmail(), plan.getName());
        } catch (Exception e) {
            log.error("Failed to send plan subscription success email for driver {}: {}",
                    driver.getUserId(), e.getMessage(), e);
        }
    }

    @Async
    public void sendPlanRenewalSuccessEmail(Driver driver, Plan plan, double fee) {
        try {
            User user = driver.getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for driver {}", driver.getUserId());
                return;
            }

            String subject = "✅ Gia hạn gói cước thành công";
            String htmlContent = buildPlanRenewalSuccessEmailTemplate(user, plan, fee);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent plan renewal success email to {} for plan {}", user.getEmail(), plan.getName());
        } catch (Exception e) {
            log.error("Failed to send plan renewal success email for driver {}: {}",
                    driver.getUserId(), e.getMessage(), e);
        }
    }

    @Async
    public void sendPlanRenewalFailedEmail(Driver driver, Plan oldPlan, Plan newPlan, double requiredFee) {
        try {
            User user = driver.getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("Cannot send email: User or email is null for driver {}", driver.getUserId());
                return;
            }

            String subject = "⚠️ Gia hạn gói cước thất bại";
            String htmlContent = buildPlanRenewalFailedEmailTemplate(user, oldPlan, newPlan, requiredFee);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("Sent plan renewal failed email to {} - downgraded from {} to {}",
                    user.getEmail(), oldPlan.getName(), newPlan.getName());
        } catch (Exception e) {
            log.error("Failed to send plan renewal failed email for driver {}: {}",
                    driver.getUserId(), e.getMessage(), e);
        }
    }

    private String buildPlanSubscriptionSuccessEmailTemplate(User user, Plan plan, double fee) {
        String userName = user.getFullName();
        String planName = plan.getName();
        String feeStr = fee > 0 ? currencyFormatter.format(fee) + " VNĐ" : "Miễn phí";
        String pricePerKwh = currencyFormatter.format(plan.getPricePerKwh()) + " VNĐ/kWh";

        String bodyContent = String.format(
            "<p>Chúc mừng bạn đã đăng ký gói cước <strong>%s</strong> thành công!</p>" +
            "<div style='background:#f0f8ff;padding:15px;border-left:4px solid #15919B;margin:20px 0'>" +
            "<h3 style='margin-top:0;color:#15919B'>📋 Chi tiết gói cước</h3>" +
            "<ul style='margin:10px 0'>" +
            "<li><strong>Gói:</strong> %s</li>" +
            "<li><strong>Phí hàng tháng:</strong> %s</li>" +
            "<li><strong>Giá điện:</strong> %s</li>" +
            "<li><strong>Lợi ích:</strong> %s</li>" +
            "</ul></div>" +
            "<p>Số tiền <strong>%s</strong> đã được trừ từ ví của bạn.</p>" +
            "<p>Gói cước sẽ tự động gia hạn vào tháng sau nếu là gói <strong>Cao cấp</strong> hoặc <strong>Tiết kiệm</strong>.</p>",
            planName, planName, feeStr, pricePerKwh, plan.getBenefits() != null ? plan.getBenefits() : "Không có",
            feeStr
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }

    private String buildPlanRenewalSuccessEmailTemplate(User user, Plan plan, double fee) {
        String userName = user.getFullName();
        String planName = plan.getName();
        String feeStr = currencyFormatter.format(fee) + " VNĐ";

        String bodyContent = String.format(
            "<p>Gói cước <strong>%s</strong> của bạn đã được gia hạn thành công!</p>" +
            "<div style='background:#f0fff0;padding:15px;border-left:4px solid #28a745;margin:20px 0'>" +
            "<ul style='margin:10px 0'>" +
            "<li><strong>Gói:</strong> %s</li>" +
            "<li><strong>Phí đã thanh toán:</strong> %s</li>" +
            "<li><strong>Ngày gia hạn:</strong> %s</li>" +
            "</ul></div>" +
            "<p>Gói cước của bạn sẽ tiếp tục có hiệu lực trong 30 ngày tới.</p>",
            planName, planName, feeStr,
            java.time.LocalDateTime.now().format(timeFormatter)
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }

    private String buildPlanRenewalFailedEmailTemplate(User user, Plan oldPlan, Plan newPlan, double requiredFee) {
        String userName = user.getFullName();
        String oldPlanName = oldPlan.getName();
        String newPlanName = newPlan != null ? newPlan.getName() : "Linh hoạt";
        String feeStr = currencyFormatter.format(requiredFee) + " VNĐ";

        String bodyContent = String.format(
            "<p>Rất tiếc, gia hạn gói cước <strong>%s</strong> của bạn đã thất bại do số dư ví không đủ.</p>" +
            "<div style='background:#fff3cd;padding:15px;border-left:4px solid #ffc107;margin:20px 0'>" +
            "<h3 style='margin-top:0;color:#856404'>⚠️ Thông báo quan trọng</h3>" +
            "<ul style='margin:10px 0'>" +
            "<li><strong>Gói cũ:</strong> %s</li>" +
            "<li><strong>Phí yêu cầu:</strong> %s</li>" +
            "<li><strong>Gói mới:</strong> %s (Tự động chuyển)</li>" +
            "</ul></div>" +
            "<p>Hệ thống đã tự động chuyển bạn sang gói <strong>%s</strong>.</p>" +
            "<p>Vui lòng <strong>nạp thêm tiền</strong> vào ví để tiếp tục sử dụng gói cước cao cấp.</p>",
            oldPlanName, oldPlanName, feeStr, newPlanName, newPlanName
        );

        return buildBaseEmailTemplate(userName, bodyContent);
    }
}

