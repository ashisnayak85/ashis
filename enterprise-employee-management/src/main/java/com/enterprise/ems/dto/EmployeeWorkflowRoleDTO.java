package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWorkflowRoleDTO {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeCode;

    private Long workflowRoleId;
    private String workflowRoleName;

    // Null means global/company-wide - see EmployeeWorkflowRole entity javadoc.
    private Long scopeDepartmentId;
    private String scopeDepartmentName;

    private Boolean active;

    private LocalDateTime assignedAt;
    private Long assignedByEmployeeId;
    private String assignedByName;

    private LocalDateTime removedAt;
    private Long removedByEmployeeId;
    private String removedByName;
}
