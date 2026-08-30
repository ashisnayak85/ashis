package com.enterprise.ca.service.impl;

import com.enterprise.ca.entity.AuditLog;
import com.enterprise.ca.repository.AuditLogRepository;
import com.enterprise.ca.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String action, String entityType, Long entityId, String description) {
        String actor = "system";
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) actor = auth.getName();
        } catch (Exception ignored) { }

        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .performedBy(actor)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
