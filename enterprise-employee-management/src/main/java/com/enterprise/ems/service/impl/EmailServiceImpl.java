package com.enterprise.ems.service.impl;

import com.enterprise.ems.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/*
 * PURPOSE: Email Module (Phase - Email)
 * Uses Spring Mail with async sending for non-blocking operations
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@eems.com}")
    private String fromEmail;

    @Override
    @Async
    public void sendWelcomeEmail(String to, String username) {
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
    public void sendPasswordResetEmail(String to, String resetLink) {
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
