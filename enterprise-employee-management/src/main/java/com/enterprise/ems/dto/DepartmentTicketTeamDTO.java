package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentTicketTeamDTO {

    private Long id;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Employee is required")
    private Long employeeId;

    private String employeeName;

    // MEMBER or ESCALATION
    @NotBlank(message = "Role in team is required")
    private String roleInTeam;
}
