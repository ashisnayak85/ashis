package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTypeDTO {
	private Long id;

    @NotBlank(message = "Employee Type name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Employee Type code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric")
    private String code;

    @Size(max = 500)
    private String description;

    private Boolean active;
}
