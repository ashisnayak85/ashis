package com.enterprise.ems.service.impl;

import com.enterprise.ems.entity.AuditLog;
import com.enterprise.ems.repository.AuditLogRepository;
import com.enterprise.ems.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/*
 * PURPOSE: Async audit logging - does not block main request thread
 * ANNOTATION: @Async - method runs in separate thread pool
 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(String action, String entityType, Long entityId, String details) {
        String performedBy = "SYSTEM";
        try {
            performedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {
            // No authenticated user (e.g., login failure)
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.info("AUDIT: {} {} {} by {}", action, entityType, entityId, performedBy);
    }
}
