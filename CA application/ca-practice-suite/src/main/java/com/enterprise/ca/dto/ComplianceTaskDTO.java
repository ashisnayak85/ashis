package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceTaskDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Task type is required")
    private String taskType;

    private Long clientId;
    private String clientName;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String status;
    private LocalDate completedDate;
    private String assignedTo;
    private String remarks;
}
