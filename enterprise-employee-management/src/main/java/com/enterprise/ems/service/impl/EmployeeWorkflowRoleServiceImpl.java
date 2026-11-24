package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.EmployeeWorkflowRoleDTO;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.EmployeeWorkflowRole;
import com.enterprise.ems.entity.WorkflowRole;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.WorkflowRoleMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.EmployeeWorkflowRoleRepository;
import com.enterprise.ems.repository.WorkflowRoleRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.EmployeeWorkflowRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeWorkflowRoleServiceImpl implements EmployeeWorkflowRoleService {

    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;
    private final WorkflowRoleRepository workflowRoleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkflowRoleMapper mapper;
    private final AuditService auditService;

    @Override
    public EmployeeWorkflowRoleDTO assign(Long employeeId, Long workflowRoleId, Long scopeDepartmentId, Long assignedByEmployeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        WorkflowRole workflowRole = workflowRoleRepository.findById(workflowRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow role not found: " + workflowRoleId));
        if (!workflowRole.getActive()) {
            throw new BusinessException("Cannot assign an inactive workflow role - reactivate it first");
        }

        Department scopeDepartment = null;
        if (scopeDepartmentId != null) {
            scopeDepartment = departmentRepository.findById(scopeDepartmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + scopeDepartmentId));
        }

        boolean alreadyHeld = employeeWorkflowRoleRepository
                .findByEmployeeIdAndWorkflowRoleIdAndScopeDepartmentIdAndActiveTrue(
                        employeeId, workflowRoleId, scopeDepartmentId)
                .isPresent();
        if (alreadyHeld) {
            throw new BusinessException(employee.getFullName() + " already holds this role in the given scope");
        }

        Employee assignedBy = assignedByEmployeeId != null
                ? employeeRepository.findById(assignedByEmployeeId).orElse(null)
                : null;

        EmployeeWorkflowRole assignment = employeeWorkflowRoleRepository.save(EmployeeWorkflowRole.builder()
                .employee(employee)
                .workflowRole(workflowRole)
                .scopeDepartment(scopeDepartment)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy)
                .build());

        auditService.log("ASSIGN", "EmployeeWorkflowRole", assignment.getId(),
                workflowRole.getName() + " assigned to " + employee.getFullName()
                        + (scopeDepartment != null ? " (scope: " + scopeDepartment.getName() + ")" : " (global)"));
        return mapper.toDTO(assignment);
    }

    @Override
    public EmployeeWorkflowRoleDTO revoke(Long assignmentId, Long removedByEmployeeId) {
        EmployeeWorkflowRole assignment = employeeWorkflowRoleRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found: " + assignmentId));
        if (!assignment.getActive()) {
            throw new BusinessException("This role assignment has already been removed");
        }

        Employee removedBy = removedByEmployeeId != null
                ? employeeRepository.findById(removedByEmployeeId).orElse(null)
                : null;

        assignment.setActive(false);
        assignment.setRemovedAt(LocalDateTime.now());
        assignment.setRemovedBy(removedBy);
        employeeWorkflowRoleRepository.save(assignment);

        auditService.log("REVOKE", "EmployeeWorkflowRole", assignment.getId(),
                assignment.getWorkflowRole().getName() + " removed from " + assignment.getEmployee().getFullName());
        return mapper.toDTO(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeWorkflowRoleDTO> getForEmployee(Long employeeId) {
        return employeeWorkflowRoleRepository.findByEmployeeIdOrderByAssignedAtDesc(employeeId)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeWorkflowRoleDTO> getForRole(Long workflowRoleId) {
        return employeeWorkflowRoleRepository.findByWorkflowRoleIdAndActiveTrueOrderByIdAsc(workflowRoleId)
                .stream().map(mapper::toDTO).toList();
    }
}
