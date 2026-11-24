package com.enterprise.ems.service.hiring;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.EmployeeWorkflowRole;
import com.enterprise.ems.entity.WorkflowStage;
import com.enterprise.ems.repository.EmployeeWorkflowRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/*
 * PURPOSE: Resolves the actual approver Employee for one WorkflowStage - the same
 * rules ResignationServiceImpl.resolveApprover() uses, extracted so the
 * requisition and offer workflows can share them WITHOUT touching the working
 * Resignation code. (Once a third consumer appears, migrate Resignation onto this too.)
 *
 * "subject" is the employee the chain is relative to:
 *   requisition -> the person who raised it
 *   offer       -> the requisition's hiring manager
 * Returns null (never throws) when nobody eligible is found, so the caller can
 * record the stage as SKIPPED instead of blocking the whole workflow.
 */
@Component
@RequiredArgsConstructor
public class WorkflowApproverResolver {

    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;

    public Employee resolve(WorkflowStage stage, Employee subject) {
        return switch (stage.getApproverType()) {
            case AppConstants.APPROVER_TYPE_REPORTING_MANAGER -> subject.getReportingManager();

            case AppConstants.APPROVER_TYPE_ROLE_IN_DEPARTMENT -> {
                if (stage.getWorkflowRole() == null) yield null;
                Long departmentId = "FIXED_DEPARTMENT".equals(stage.getDepartmentScope()) && stage.getFixedDepartment() != null
                        ? stage.getFixedDepartment().getId()
                        : (subject.getDepartment() != null ? subject.getDepartment().getId() : null);
                if (departmentId == null) yield null;
                List<EmployeeWorkflowRole> candidates = employeeWorkflowRoleRepository
                        .findByWorkflowRoleIdAndScopeDepartmentIdAndActiveTrueOrderByIdAsc(stage.getWorkflowRole().getId(), departmentId);
                yield candidates.stream()
                        .map(EmployeeWorkflowRole::getEmployee)
                        .filter(e -> !e.getId().equals(subject.getId())) // nobody approves their own request
                        .findFirst()
                        .orElse(null);
            }

            case AppConstants.APPROVER_TYPE_SPECIFIC_ROLE -> {
                if (stage.getWorkflowRole() == null) yield null;
                List<EmployeeWorkflowRole> candidates = employeeWorkflowRoleRepository
                        .findByWorkflowRoleIdAndScopeDepartmentIsNullAndActiveTrueOrderByIdAsc(stage.getWorkflowRole().getId());
                yield candidates.stream()
                        .map(EmployeeWorkflowRole::getEmployee)
                        .filter(e -> !e.getId().equals(subject.getId()))
                        .findFirst()
                        .orElse(null);
            }

            case AppConstants.APPROVER_TYPE_SPECIFIC_USER -> stage.getSpecificEmployee();

            default -> null;
        };
    }
}
