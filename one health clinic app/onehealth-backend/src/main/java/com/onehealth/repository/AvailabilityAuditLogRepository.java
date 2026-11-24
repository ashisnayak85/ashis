package com.onehealth.repository;

import com.onehealth.entity.AvailabilityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityAuditLogRepository extends JpaRepository<AvailabilityAuditLog, Long> {
    List<AvailabilityAuditLog> findByOrganizationIdOrderByPerformedAtDesc(Long organizationId);
    List<AvailabilityAuditLog> findByOrganizationIdAndClinicIdOrderByPerformedAtDesc(Long organizationId, Long clinicId);
}
