package com.enterprise.ca.service;

public interface AuditService {
    void log(String action, String entityType, Long entityId, String description);
}
