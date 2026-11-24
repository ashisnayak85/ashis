package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicyDTO {

    private Long id;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotNull(message = "Acceptance hours is required")
    @Min(value = 1, message = "Must be at least 1 hour")
    private Integer acceptanceHours;

    @NotNull(message = "Resolution hours is required")
    @Min(value = 1, message = "Must be at least 1 hour")
    private Integer resolutionHours;
}
