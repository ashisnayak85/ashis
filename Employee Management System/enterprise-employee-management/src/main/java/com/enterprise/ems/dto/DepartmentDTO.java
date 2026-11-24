package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDTO {

    private Long id;

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric")
    private String code;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private Long employeeCount;

    // Head of Department - who tickets for this department ultimately
    // escalate to. Nullable (see Department entity's comment).
    private Long headOfDepartmentId;
    private String headOfDepartmentName;
}
