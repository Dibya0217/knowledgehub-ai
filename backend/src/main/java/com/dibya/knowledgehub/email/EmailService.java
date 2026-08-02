package com.dibya.knowledgehub.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Verify your KnowledgeHub AI account");
            helper.setText(buildVerificationHtml(name, otp, toEmail), true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Reset your KnowledgeHub AI password");
            helper.setText(buildPasswordResetHtml(name, otp), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationHtml(String name, String otp, String email) {
        String verifyUrl = frontendUrl + "/verify-email?email=" + email;
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#0f0f17;font-family:Inter,system-ui,sans-serif;">
                  <div style="max-width:520px;margin:40px auto;background:#1c1c2d;border-radius:16px;overflow:hidden;border:1px solid rgba(255,255,255,0.08);">
                    <div style="background:linear-gradient(135deg,#6366f1,#a855f7);padding:32px;text-align:center;">
                      <h1 style="margin:0;color:#fff;font-size:24px;font-weight:700;">KnowledgeHub AI</h1>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#fff;margin:0 0 8px;">Welcome, %s!</h2>
                      <p style="color:#a0a0b9;margin:0 0 24px;">Use the code below to verify your email address. It expires in 15 minutes.</p>
                      <div style="background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.3);border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                        <p style="margin:0;color:#a0a0b9;font-size:12px;text-transform:uppercase;letter-spacing:2px;">Verification Code</p>
                        <p style="margin:8px 0 0;color:#fff;font-size:36px;font-weight:700;letter-spacing:8px;">%s</p>
                      </div>
                      <a href="%s" style="display:block;background:linear-gradient(135deg,#6366f1,#a855f7);color:#fff;text-decoration:none;padding:14px;border-radius:12px;text-align:center;font-weight:600;margin-bottom:16px;">
                        Open Verification Page
                      </a>
                      <p style="color:#a0a0b9;font-size:12px;text-align:center;margin:0;">If you didn't create this account, ignore this email.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, otp, verifyUrl);
    }

    private String buildPasswordResetHtml(String name, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#0f0f17;font-family:Inter,system-ui,sans-serif;">
                  <div style="max-width:520px;margin:40px auto;background:#1c1c2d;border-radius:16px;overflow:hidden;border:1px solid rgba(255,255,255,0.08);">
                    <div style="background:linear-gradient(135deg,#6366f1,#a855f7);padding:32px;text-align:center;">
                      <h1 style="margin:0;color:#fff;font-size:24px;font-weight:700;">KnowledgeHub AI</h1>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#fff;margin:0 0 8px;">Password Reset, %s</h2>
                      <p style="color:#a0a0b9;margin:0 0 24px;">Use the code below to reset your password. It expires in 5 minutes.</p>
                      <div style="background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.3);border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                        <p style="margin:0;color:#a0a0b9;font-size:12px;text-transform:uppercase;letter-spacing:2px;">Reset Code</p>
                        <p style="margin:8px 0 0;color:#fff;font-size:36px;font-weight:700;letter-spacing:8px;">%s</p>
                      </div>
                      <p style="color:#a0a0b9;font-size:12px;text-align:center;margin:0;">If you didn't request a password reset, ignore this email.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, otp);
    }
}
