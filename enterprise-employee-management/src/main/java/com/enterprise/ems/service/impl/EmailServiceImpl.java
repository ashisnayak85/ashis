package com.enterprise.ems.service.impl;

import com.enterprise.ems.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/*
 * PURPOSE: Email Module (Phase - Email)
 * Uses Spring Mail with async sending for non-blocking operations.
 *
 * IMPORTANT: JavaMailSender is looked up lazily via ObjectProvider instead of
 * @ConditionalOnBean. @ConditionalOnBean on a plain @Service is unreliable here:
 * regular component-scanned beans are evaluated BEFORE Spring Boot's mail
 * auto-configuration finishes registering JavaMailSender, so the condition can
 * see "no bean yet" even when spring.mail.host IS configured - silently
 * skipping this whole class with no error. ObjectProvider defers the lookup
 * until send time, so it always sees the real, final state.
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:noreply@eems.com}")
    private String fromEmail;

    @Override
    @Async
    public void sendWelcomeEmail(String to, String username) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is not configured (no active profile with spring.mail.host) - skipping welcome email to {}", to);
            return;
        }
        log.info("Sending welcome email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Welcome to Enterprise Employee Management System");
            message.setText("Hello " + username + ",\n\nWelcome to EEMS! Your account has been created successfully.\n\nRegards,\nEEMS Team");
            mailSender.send(message);
            log.debug("Welcome email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendCredentialsEmail(String to, String username, String temporaryPassword) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is not configured (no active profile with spring.mail.host) - skipping credentials email to {}", to);
            return;
        }
        log.info("Sending credentials email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Welcome to EEMS - Your login access");
            message.setText("Hello " + username + ",\n\n"
                    + "An admin has given you access to the Enterprise Employee Management System.\n\n"
                    + "Username: " + username + "\n"
                    + "Temporary password: " + temporaryPassword + "\n\n"
                    + "Please log in and change this password immediately from your profile page.\n\n"
                    + "Regards,\nEEMS Team");
            mailSender.send(message);
            log.debug("Credentials email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is not configured - skipping password reset email to {}", to);
            return;
        }
        log.info("Sending password reset email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Password Reset Request - EEMS");
            message.setText("Click the link below to reset your password:\n\n" + resetLink + "\n\nThis link expires in 24 hours.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
        }
    }

    @Override
    @Async
    public void sendLeaveApprovalEmail(String to, String employeeName, String status) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is not configured - skipping leave {} email to {}", status, to);
            return;
        }
        log.info("Sending leave {} email to: {}", status, to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Leave Request " + status);
            message.setText("Dear " + employeeName + ",\n\nYour leave request has been " + status + ".\n\nRegards,\nHR Team");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send leave email", e);
        }
    }
}
