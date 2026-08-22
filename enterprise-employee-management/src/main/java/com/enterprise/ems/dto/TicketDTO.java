package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDTO {

    private Long id;

    private String ticketNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000)
    private String description;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;

    private Long raisedById;
    private String raisedByName;

    private Long assignedToId;
    private String assignedToName;

    private String status;

    // LOW / MEDIUM / HIGH - optional on create (defaults to MEDIUM server-side)
    private String priority;

    private Integer escalationLevel;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    private Boolean acceptanceBreached;
    private Boolean resolutionBreached;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed, not persisted: does the requesting employee have unread
    // activity on this ticket (see TicketServiceImpl.hasUnread).
    private Boolean hasUnread;
}
