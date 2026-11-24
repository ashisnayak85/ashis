package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.WorkflowRoleDTO;
import com.enterprise.ems.entity.WorkflowRole;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.WorkflowRoleMapper;
import com.enterprise.ems.repository.EmployeeWorkflowRoleRepository;
import com.enterprise.ems.repository.WorkflowRoleRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.WorkflowRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowRoleServiceImpl implements WorkflowRoleService {

    private final WorkflowRoleRepository workflowRoleRepository;
    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;
    private final WorkflowRoleMapper mapper;
    private final AuditService auditService;

    @Override
    public WorkflowRoleDTO create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Role name is required");
        }
        if (workflowRoleRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessException("A workflow role named '" + name.trim() + "' already exists");
        }
        WorkflowRole role = workflowRoleRepository.save(WorkflowRole.builder()
                .name(name.trim())
                .description(description)
                .build());
        auditService.log("CREATE", "WorkflowRole", role.getId(), "Workflow role created: " + role.getName());
        return mapper.toDTO(role);
    }

    @Override
    public WorkflowRoleDTO update(Long id, String name, String description) {
        WorkflowRole role = getOrThrow(id);
        if (name != null && !name.isBlank() && !name.trim().equalsIgnoreCase(role.getName())
                && workflowRoleRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessException("A workflow role named '" + name.trim() + "' already exists");
        }
        if (name != null && !name.isBlank()) {
            role.setName(name.trim());
        }
        role.setDescription(description);
        workflowRoleRepository.save(role);
        auditService.log("UPDATE", "WorkflowRole", role.getId(), "Workflow role updated: " + role.getName());
        return mapper.toDTO(role);
    }

    @Override
    public WorkflowRoleDTO deactivate(Long id) {
        WorkflowRole role = getOrThrow(id);
        // Guardrail: don't let admin silently orphan a stage or active
        // assignment that still points at this role. Force them to retire
        // those first - cheap to check, expensive to debug a stage that
        // suddenly SKIPs everything for an undiscoverable reason.
        if (employeeWorkflowRoleRepository.existsByWorkflowRoleIdAndActiveTrue(id)) {
            throw new BusinessException("This role still has active employee assignments - remove those first before deactivating the role");
        }
        role.setActive(false);
        workflowRoleRepository.save(role);
        auditService.log("DEACTIVATE", "WorkflowRole", role.getId(), "Workflow role deactivated: " + role.getName());
        return mapper.toDTO(role);
    }

    @Override
    public WorkflowRoleDTO reactivate(Long id) {
        WorkflowRole role = getOrThrow(id);
        role.setActive(true);
        workflowRoleRepository.save(role);
        auditService.log("REACTIVATE", "WorkflowRole", role.getId(), "Workflow role reactivated: " + role.getName());
        return mapper.toDTO(role);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowRoleDTO getById(Long id) {
        return mapper.toDTO(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowRoleDTO> getAll(boolean activeOnly) {
        List<WorkflowRole> roles = activeOnly
                ? workflowRoleRepository.findByActiveTrueOrderByNameAsc()
                : workflowRoleRepository.findAll();
        return roles.stream().map(mapper::toDTO).toList();
    }

    private WorkflowRole getOrThrow(Long id) {
        return workflowRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow role not found: " + id));
    }
}
