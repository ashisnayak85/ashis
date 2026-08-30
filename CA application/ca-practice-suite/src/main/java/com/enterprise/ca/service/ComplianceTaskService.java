package com.enterprise.ca.service;

import com.enterprise.ca.dto.ComplianceTaskDTO;
import com.enterprise.ca.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ComplianceTaskService {
    ComplianceTaskDTO create(ComplianceTaskDTO dto);
    ComplianceTaskDTO update(Long id, ComplianceTaskDTO dto);
    ComplianceTaskDTO markFiled(Long id, String remarks);
    void delete(Long id);
    PageResponse<ComplianceTaskDTO> search(Long clientId, String status, String taskType, Pageable pageable);
    java.util.List<ComplianceTaskDTO> getUpcoming(int limit);
}
