package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.TicketConversationDTO;
import com.enterprise.ems.dto.TicketDTO;
import com.enterprise.ems.entity.TicketConversation;
import com.enterprise.ems.entity.TicketMaster;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketDTO toDTO(TicketMaster entity, boolean hasUnread) {
        if (entity == null) return null;
        return TicketDTO.builder()
                .id(entity.getId())
                .ticketNumber(entity.getTicketNumber())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .departmentId(entity.getDepartment().getId())
                .departmentName(entity.getDepartment().getName())
                .raisedById(entity.getRaisedBy().getId())
                .raisedByName(entity.getRaisedBy().getFullName())
                .assignedToId(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)
                .assignedToName(entity.getAssignedTo() != null ? entity.getAssignedTo().getFullName() : null)
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .escalationLevel(entity.getEscalationLevel())
                .assignedAt(entity.getAssignedAt())
                .acceptedAt(entity.getAcceptedAt())
                .resolvedAt(entity.getResolvedAt())
                .closedAt(entity.getClosedAt())
                .acceptanceBreached(entity.getAcceptanceBreached())
                .resolutionBreached(entity.getResolutionBreached())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .hasUnread(hasUnread)
                .build();
    }

    public TicketConversationDTO toDTO(TicketConversation entity) {
        if (entity == null) return null;
        return TicketConversationDTO.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket().getId())
                .authorId(entity.getAuthor() != null ? entity.getAuthor().getId() : null)
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getFullName() : null)
                .entryType(entity.getEntryType())
                .message(entity.getMessage())
                .targetEmployeeId(entity.getTargetEmployee() != null ? entity.getTargetEmployee().getId() : null)
                .targetEmployeeName(entity.getTargetEmployee() != null ? entity.getTargetEmployee().getFullName() : null)
                .parentEntryId(entity.getParentEntry() != null ? entity.getParentEntry().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
