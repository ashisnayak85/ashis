package com.enterprise.ems.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String username);

    void sendPasswordResetEmail(String to, String resetLink);

    void sendLeaveApprovalEmail(String to, String employeeName, String status);
}
