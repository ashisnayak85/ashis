package com.enterprise.ems.service;

import com.enterprise.ems.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketService {

    // --- Raising / viewing ---

    TicketDTO createTicket(TicketDTO dto, Long raisedByEmployeeId);

    // requesterEmployeeId/privileged drive both authorization (can this person
    // see this ticket at all) and the computed hasUnread flag.
    TicketDTO getById(Long ticketId, Long requesterEmployeeId, boolean privileged);

    List<TicketConversationDTO> getConversation(Long ticketId, Long requesterEmployeeId, boolean privileged);

    PageResponse<TicketDTO> getMyTickets(Long employeeId, String status, String priority, Pageable pageable);

    PageResponse<TicketDTO> getAssignedTickets(Long employeeId, String status, String priority, Pageable pageable);

    // The unclaimed pool: OPEN + assignedTo IS NULL, restricted to the
    // departments this employee is on the ticket team for. This is what a
    // MEMBER/ESCALATION team member browses to find something to claim -
    // getAssignedTickets() can't do this because assignedTo is null until
    // after claim() runs (chicken-and-egg otherwise).
    PageResponse<TicketDTO> getClaimableTickets(Long employeeId, String priority, Pageable pageable);

    // Admin/manager: unrestricted, filterable across every department.
    PageResponse<TicketDTO> searchTickets(Long departmentId, String status, String priority, String keyword, Pageable pageable);

    void markRead(Long ticketId, Long employeeId);

    // --- Conversation / actions ---

    TicketConversationDTO addReply(Long ticketId, Long authorId, String message, Long parentEntryId);

    // Responsible-person actions
    TicketDTO claim(Long ticketId, Long employeeId);
    TicketDTO resolve(Long ticketId, Long employeeId, String message);
    TicketDTO reject(Long ticketId, Long employeeId, String message);
    TicketDTO transfer(Long ticketId, Long employeeId, Long targetEmployeeId, String message);

    // Raiser (user-side) actions
    TicketDTO acceptResolution(Long ticketId, Long employeeId);
    TicketDTO escalate(Long ticketId, Long employeeId, String message);
    TicketDTO closeTicket(Long ticketId, Long employeeId, String message);

    // --- SLA scheduler entry point (see TicketSlaScheduler) ---
    void checkSlaBreaches();

    // --- Department ticket team (admin config) ---
    List<DepartmentTicketTeamDTO> getTeam(Long departmentId);
    DepartmentTicketTeamDTO addTeamMember(DepartmentTicketTeamDTO dto);
    void removeTeamMember(Long id);

    // --- SLA policy (admin config) ---
    List<SlaPolicyDTO> getSlaPolicies(Long departmentId);
    SlaPolicyDTO saveSlaPolicy(SlaPolicyDTO dto);
}
