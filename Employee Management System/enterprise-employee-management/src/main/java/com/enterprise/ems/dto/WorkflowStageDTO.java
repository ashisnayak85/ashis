package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

// Renamed from ResignationApprovalStageDTO - now carries the owning
// module too, since WorkflowStage is shared across every module's chain.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStageDTO {

    private Long id;

    private Long privilegeModuleId;
    private String privilegeModuleCode;
    private String privilegeModuleName;

    private String name;
    private Integer sequenceOrder;

    // REPORTING_MANAGER / ROLE_IN_DEPARTMENT / SPECIFIC_ROLE / SPECIFIC_USER
    private String approverType;

    // Used when approverType = ROLE_IN_DEPARTMENT or SPECIFIC_ROLE
    private Long workflowRoleId;
    private String workflowRoleName;

    // EMPLOYEE_DEPARTMENT / FIXED_DEPARTMENT - only read when
    // approverType = ROLE_IN_DEPARTMENT
    private String departmentScope;

    // Used when departmentScope = FIXED_DEPARTMENT
    private Long fixedDepartmentId;
    private String fixedDepartmentName;

    // Used when approverType = SPECIFIC_USER
    private Long specificEmployeeId;
    private String specificEmployeeName;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
