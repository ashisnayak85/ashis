package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.ComplianceTaskDTO;
import com.enterprise.ca.entity.ComplianceTask;
import org.springframework.stereotype.Component;

@Component
public class ComplianceTaskMapper {

    public ComplianceTaskDTO toDTO(ComplianceTask t) {
        return ComplianceTaskDTO.builder()
                .id(t.getId())
                .title(t.getTitle())
                .taskType(t.getTaskType().name())
                .clientId(t.getClient() != null ? t.getClient().getId() : null)
                .clientName(t.getClient() != null ? t.getClient().getName() : null)
                .frequency(t.getFrequency().name())
                .dueDate(t.getDueDate())
                .status(t.getStatus().name())
                .completedDate(t.getCompletedDate())
                .assignedTo(t.getAssignedTo())
                .remarks(t.getRemarks())
                .build();
    }
}
