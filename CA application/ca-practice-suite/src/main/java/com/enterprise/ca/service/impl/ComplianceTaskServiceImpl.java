package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.ComplianceTaskDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.entity.Client;
import com.enterprise.ca.entity.ComplianceTask;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.ComplianceTaskMapper;
import com.enterprise.ca.repository.ClientRepository;
import com.enterprise.ca.repository.ComplianceTaskRepository;
import com.enterprise.ca.repository.spec.ComplianceTaskSpecifications;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.ComplianceTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/*
 * PURPOSE: The compliance calendar. The one piece of domain logic worth
 * calling out is in markFiled(): for a recurring task type (MONTHLY /
 * QUARTERLY / HALF_YEARLY / ANNUALLY), filing one period automatically
 * schedules the next one - this is exactly the behaviour a CA firm needs
 * (GSTR-3B for August files -> September's GSTR-3B task should already be
 * sitting there, not something someone has to remember to create).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComplianceTaskServiceImpl implements ComplianceTaskService {

    private final ComplianceTaskRepository complianceTaskRepository;
    private final ClientRepository clientRepository;
    private final ComplianceTaskMapper complianceTaskMapper;
    private final AuditService auditService;

    @Override
    public ComplianceTaskDTO create(ComplianceTaskDTO dto) {
        Client client = null;
        if (dto.getClientId() != null) {
            client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + dto.getClientId()));
        }
        ComplianceTask task = ComplianceTask.builder()
                .title(dto.getTitle())
                .taskType(ComplianceTask.TaskType.valueOf(dto.getTaskType()))
                .client(client)
                .frequency(ComplianceTask.Frequency.valueOf(dto.getFrequency()))
                .dueDate(dto.getDueDate())
                .status(ComplianceTask.TaskStatus.PENDING)
                .assignedTo(dto.getAssignedTo())
                .remarks(dto.getRemarks())
                .build();
        ComplianceTask saved = complianceTaskRepository.save(task);
        auditService.log("CREATE", "ComplianceTask", saved.getId(), "Scheduled: " + saved.getTitle());
        return complianceTaskMapper.toDTO(saved);
    }

    @Override
    public ComplianceTaskDTO update(Long id, ComplianceTaskDTO dto) {
        ComplianceTask task = complianceTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance task not found: " + id));
        task.setTitle(dto.getTitle());
        task.setDueDate(dto.getDueDate());
        task.setAssignedTo(dto.getAssignedTo());
        task.setRemarks(dto.getRemarks());
        if (dto.getStatus() != null) task.setStatus(ComplianceTask.TaskStatus.valueOf(dto.getStatus()));
        return complianceTaskMapper.toDTO(complianceTaskRepository.save(task));
    }

    @Override
    public ComplianceTaskDTO markFiled(Long id, String remarks) {
        ComplianceTask task = complianceTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance task not found: " + id));
        task.setStatus(ComplianceTask.TaskStatus.FILED);
        task.setCompletedDate(LocalDate.now());
        if (remarks != null && !remarks.isBlank()) task.setRemarks(remarks);
        ComplianceTask saved = complianceTaskRepository.save(task);
        auditService.log("FILED", "ComplianceTask", id, "Filed: " + saved.getTitle());

        if (saved.getFrequency() != ComplianceTask.Frequency.ONE_TIME) {
            createNextRecurrence(saved);
        }
        return complianceTaskMapper.toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        if (!complianceTaskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Compliance task not found: " + id);
        }
        complianceTaskRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplianceTaskDTO> search(Long clientId, String status, String taskType, Pageable pageable) {
        Specification<ComplianceTask> spec = Specification.where(ComplianceTaskSpecifications.clientEquals(clientId))
                .and(ComplianceTaskSpecifications.statusEquals(status))
                .and(ComplianceTaskSpecifications.taskTypeEquals(taskType));
        Page<ComplianceTask> page = complianceTaskRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(complianceTaskMapper::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceTaskDTO> getUpcoming(int limit) {
        List<ComplianceTask.TaskStatus> openStatuses = List.of(
                ComplianceTask.TaskStatus.PENDING, ComplianceTask.TaskStatus.IN_PROGRESS, ComplianceTask.TaskStatus.OVERDUE);
        return complianceTaskRepository.findTop10ByStatusInOrderByDueDateAsc(openStatuses).stream()
                .limit(limit)
                .map(complianceTaskMapper::toDTO)
                .toList();
    }

    private void createNextRecurrence(ComplianceTask filed) {
        LocalDate nextDue = switch (filed.getFrequency()) {
            case MONTHLY -> filed.getDueDate().plusMonths(1);
            case QUARTERLY -> filed.getDueDate().plusMonths(3);
            case HALF_YEARLY -> filed.getDueDate().plusMonths(6);
            case ANNUALLY -> filed.getDueDate().plusYears(1);
            case ONE_TIME -> null;
        };
        if (nextDue == null) return;

        ComplianceTask next = ComplianceTask.builder()
                .title(filed.getTitle())
                .taskType(filed.getTaskType())
                .client(filed.getClient())
                .frequency(filed.getFrequency())
                .dueDate(nextDue)
                .status(ComplianceTask.TaskStatus.PENDING)
                .assignedTo(filed.getAssignedTo())
                .build();
        complianceTaskRepository.save(next);
    }
}
