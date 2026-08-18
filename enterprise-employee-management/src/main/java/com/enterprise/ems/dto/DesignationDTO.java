package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationDTO {

    private Long id;

    @NotBlank(message = "Designation name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private Long employeeCount;
}
