package com.enterprise.ems.service;

import com.enterprise.ems.entity.AuditLog;

public interface AuditService {

    void log(String action, String entityType, Long entityId, String details);
}
