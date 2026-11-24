package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.EmployeeWorkflowRoleDTO;
import com.enterprise.ems.dto.PrivilegeModuleDTO;
import com.enterprise.ems.dto.WorkflowStageDTO;
import com.enterprise.ems.dto.WorkflowRoleDTO;
import com.enterprise.ems.entity.EmployeeWorkflowRole;
import com.enterprise.ems.entity.PrivilegeModule;
import com.enterprise.ems.entity.WorkflowStage;
import com.enterprise.ems.entity.WorkflowRole;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRoleMapper {

    public WorkflowRoleDTO toDTO(WorkflowRole r) {
        return WorkflowRoleDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .active(r.getActive())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public EmployeeWorkflowRoleDTO toDTO(EmployeeWorkflowRole a) {
        return EmployeeWorkflowRoleDTO.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFullName())
                .employeeCode(a.getEmployee().getEmployeeCode())
                .workflowRoleId(a.getWorkflowRole().getId())
                .workflowRoleName(a.getWorkflowRole().getName())
                .scopeDepartmentId(a.getScopeDepartment() != null ? a.getScopeDepartment().getId() : null)
                .scopeDepartmentName(a.getScopeDepartment() != null ? a.getScopeDepartment().getName() : null)
                .active(a.getActive())
                .assignedAt(a.getAssignedAt())
                .assignedByEmployeeId(a.getAssignedBy() != null ? a.getAssignedBy().getId() : null)
                .assignedByName(a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : null)
                .removedAt(a.getRemovedAt())
                .removedByEmployeeId(a.getRemovedBy() != null ? a.getRemovedBy().getId() : null)
                .removedByName(a.getRemovedBy() != null ? a.getRemovedBy().getFullName() : null)
                .build();
    }

    public PrivilegeModuleDTO toDTO(PrivilegeModule m) {
        return PrivilegeModuleDTO.builder()
                .id(m.getId())
                .code(m.getCode())
                .name(m.getName())
                .description(m.getDescription())
                .active(m.getActive())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public WorkflowStageDTO toDTO(WorkflowStage s) {
        return WorkflowStageDTO.builder()
                .id(s.getId())
                .privilegeModuleId(s.getPrivilegeModule() != null ? s.getPrivilegeModule().getId() : null)
                .privilegeModuleCode(s.getPrivilegeModule() != null ? s.getPrivilegeModule().getCode() : null)
                .privilegeModuleName(s.getPrivilegeModule() != null ? s.getPrivilegeModule().getName() : null)
                .name(s.getName())
                .sequenceOrder(s.getSequenceOrder())
                .approverType(s.getApproverType())
                .workflowRoleId(s.getWorkflowRole() != null ? s.getWorkflowRole().getId() : null)
                .workflowRoleName(s.getWorkflowRole() != null ? s.getWorkflowRole().getName() : null)
                .departmentScope(s.getDepartmentScope())
                .fixedDepartmentId(s.getFixedDepartment() != null ? s.getFixedDepartment().getId() : null)
                .fixedDepartmentName(s.getFixedDepartment() != null ? s.getFixedDepartment().getName() : null)
                .specificEmployeeId(s.getSpecificEmployee() != null ? s.getSpecificEmployee().getId() : null)
                .specificEmployeeName(s.getSpecificEmployee() != null ? s.getSpecificEmployee().getFullName() : null)
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

