package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketConversationDTO {

    private Long id;
    private Long ticketId;

    private Long authorId;
    private String authorName; // null for system-generated SLA_BREACH rows

    private String entryType;
    private String message;

    private Long targetEmployeeId;
    private String targetEmployeeName;

    private Long parentEntryId;

    private LocalDateTime createdAt;
}
