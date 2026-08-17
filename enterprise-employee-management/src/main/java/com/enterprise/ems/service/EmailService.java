package com.enterprise.ems.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String username);

    // Sends the system-generated, one-time password to a newly granted user.
    // The admin never sees or sets this password - only this email carries it.
    void sendCredentialsEmail(String to, String username, String temporaryPassword);

    void sendPasswordResetEmail(String to, String resetLink);

    void sendLeaveApprovalEmail(String to, String employeeName, String status);
}
