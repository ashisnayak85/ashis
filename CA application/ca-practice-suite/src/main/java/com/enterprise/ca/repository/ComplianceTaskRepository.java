package com.enterprise.ca.repository;

import com.enterprise.ca.entity.ComplianceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface ComplianceTaskRepository extends JpaRepository<ComplianceTask, Long>, JpaSpecificationExecutor<ComplianceTask> {
    List<ComplianceTask> findByStatusAndDueDateBefore(ComplianceTask.TaskStatus status, LocalDate date);
    List<ComplianceTask> findTop10ByStatusInOrderByDueDateAsc(List<ComplianceTask.TaskStatus> statuses);
    long countByStatus(ComplianceTask.TaskStatus status);
    long countByClientId(Long clientId);
}
