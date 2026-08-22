package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.TicketMapper;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.repository.spec.TicketSpecifications;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * ================================================================================
 * TICKETING MODULE - state machine reference
 * ================================================================================
 * OPEN -> claim() -> IN_PROGRESS -> resolve()/reject() -> RESOLVED/REJECTED
 *   RESOLVED/REJECTED -> acceptResolution() -> CLOSED (terminal)
 *   RESOLVED/REJECTED -> escalate() -> OPEN (reassigned, escalationLevel++)
 *   OPEN/IN_PROGRESS -> closeTicket() -> CLOSED (raiser self-withdraws)
 *   IN_PROGRESS -> transfer() -> OPEN (new assignee must claim it fresh)
 * reply() never changes status - it's a REPLY conversation row only.
 *
 * TWO SLA CLOCKS, NOT ONE:
 *   acceptance:  assignedAt -> acceptedAt   ("did they even pick it up?")
 *   resolution:  acceptedAt -> resolvedAt   ("did they solve it in time?")
 * Conflating these into one timestamp would hide which failure actually
 * happened - see checkSlaBreaches() / TicketSlaScheduler.
 * ================================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    // Built-in fallback SLA (hours) used when no SlaPolicy row exists yet for
    // a department+priority - keeps the feature usable before an admin
    // configures anything. {acceptanceHours, resolutionHours}.
    private static final Map<String, int[]> DEFAULT_SLA_HOURS = Map.of(
            "HIGH", new int[]{4, 24},
            "MEDIUM", new int[]{24, 48},
            "LOW", new int[]{48, 72}
    );

    private final TicketMasterRepository ticketRepository;
    private final TicketConversationRepository conversationRepository;
    private final TicketReadStatusRepository readStatusRepository;
    private final DepartmentTicketTeamRepository teamRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketMapper ticketMapper;
    private final AuditService auditService;

    // ============================================================
    // Raising / viewing
    // ============================================================

    @Override
    public TicketDTO createTicket(TicketDTO dto, Long raisedByEmployeeId) {
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));
        Employee raisedBy = employeeRepository.findById(raisedByEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + raisedByEmployeeId));

        String priority = normalizePriority(dto.getPriority());
        LocalDateTime now = LocalDateTime.now();

        TicketMaster ticket = TicketMaster.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .department(department)
                .raisedBy(raisedBy)
                .status("OPEN")
                .priority(priority)
                .escalationLevel(0)
                .assignedAt(now) // acceptance clock starts immediately - it's in the pool now
                .build();
        ticket = ticketRepository.save(ticket);

        // Ticket number needs the id, so it's set in a second write - simple
        // and avoids a separate sequence/counter table for a human-readable label.
        ticket.setTicketNumber("TKT-" + String.format("%06d", ticket.getId()));
        ticket = ticketRepository.save(ticket);

        auditService.log("CREATE", "Ticket", ticket.getId(), "Ticket raised: " + ticket.getTicketNumber() + " by " + raisedBy.getFullName());
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDTO getById(Long ticketId, Long requesterEmployeeId, boolean privileged) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        authorizeView(ticket, requesterEmployeeId, privileged);
        return ticketMapper.toDTO(ticket, hasUnread(ticket, requesterEmployeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketConversationDTO> getConversation(Long ticketId, Long requesterEmployeeId, boolean privileged) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        authorizeView(ticket, requesterEmployeeId, privileged);
        return conversationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(ticketMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketDTO> getMyTickets(Long employeeId, String status, String priority, Pageable pageable) {
        Specification<TicketMaster> spec = Specification.allOf(
                TicketSpecifications.raisedBy(employeeId),
                TicketSpecifications.hasStatus(status),
                TicketSpecifications.hasPriority(priority));
        return toPageResponse(ticketRepository.findAll(spec, pageable), employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketDTO> getAssignedTickets(Long employeeId, String status, String priority, Pageable pageable) {
        Specification<TicketMaster> spec = Specification.allOf(
                TicketSpecifications.assignedTo(employeeId),
                TicketSpecifications.hasStatus(status),
                TicketSpecifications.hasPriority(priority));
        return toPageResponse(ticketRepository.findAll(spec, pageable), employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketDTO> getClaimableTickets(Long employeeId, String priority, Pageable pageable) {
        List<Long> myDepartmentIds = teamRepository.findDepartmentIdsByEmployeeId(employeeId);
        Specification<TicketMaster> spec = Specification.allOf(
                TicketSpecifications.inDepartments(myDepartmentIds),
                TicketSpecifications.hasStatus("OPEN"),
                TicketSpecifications.isUnassigned(),
                TicketSpecifications.hasPriority(priority));
        return toPageResponse(ticketRepository.findAll(spec, pageable), employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketDTO> searchTickets(Long departmentId, String status, String priority, String keyword, Pageable pageable) {
        Specification<TicketMaster> spec = Specification.allOf(
                TicketSpecifications.inDepartment(departmentId),
                TicketSpecifications.hasStatus(status),
                TicketSpecifications.hasPriority(priority),
                TicketSpecifications.keywordLike(keyword));
        return toPageResponse(ticketRepository.findAll(spec, pageable), null);
    }

    @Override
    public void markRead(Long ticketId, Long employeeId) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        TicketReadStatus readStatus = readStatusRepository.findByTicketIdAndEmployeeId(ticketId, employeeId)
                .orElseGet(() -> TicketReadStatus.builder().ticket(ticket).employee(employee).build());
        readStatus.setLastReadAt(LocalDateTime.now());
        readStatusRepository.save(readStatus);
    }

    // ============================================================
    // Conversation / actions
    // ============================================================

    @Override
    public TicketConversationDTO addReply(Long ticketId, Long authorId, String message, Long parentEntryId) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        if ("CLOSED".equals(ticket.getStatus())) {
            throw new BusinessException("This ticket is closed - no further replies are possible");
        }
        Employee author = employeeRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + authorId));
        requireInvolved(ticket, authorId);

        TicketConversation parent = parentEntryId != null
                ? conversationRepository.findById(parentEntryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Conversation entry not found: " + parentEntryId))
                : null;

        TicketConversation entry = conversationRepository.save(TicketConversation.builder()
                .ticket(ticket)
                .author(author)
                .entryType("REPLY")
                .message(message)
                .parentEntry(parent)
                .build());
        return ticketMapper.toDTO(entry);
    }

    @Override
    public TicketDTO claim(Long ticketId, Long employeeId) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        if (!"OPEN".equals(ticket.getStatus())) {
            throw new BusinessException("Only an OPEN ticket can be claimed (current status: " + ticket.getStatus() + ")");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!teamRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), employeeId)) {
            throw new BusinessException("Only a member of this department's ticket team can claim this ticket");
        }

        ticket.setAssignedTo(employee);
        ticket.setStatus("IN_PROGRESS");
        ticket.setAcceptedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(employee).entryType("CLAIM").build());
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO resolve(Long ticketId, Long employeeId, String message) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireAssignee(ticket, employeeId);
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new BusinessException("Only an IN_PROGRESS ticket can be resolved (current status: " + ticket.getStatus() + ")");
        }
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(ticket.getAssignedTo()).entryType("RESOLVE").message(message).build());
        auditService.log("RESOLVE", "Ticket", ticket.getId(), ticket.getTicketNumber() + " resolved");
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO reject(Long ticketId, Long employeeId, String message) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireAssignee(ticket, employeeId);
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new BusinessException("Only an IN_PROGRESS ticket can be rejected (current status: " + ticket.getStatus() + ")");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessException("A reason is required to reject a ticket");
        }
        ticket.setStatus("REJECTED");
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(ticket.getAssignedTo()).entryType("REJECT").message(message).build());
        auditService.log("REJECT", "Ticket", ticket.getId(), ticket.getTicketNumber() + " rejected: " + message);
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO transfer(Long ticketId, Long employeeId, Long targetEmployeeId, String message) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireAssignee(ticket, employeeId);
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new BusinessException("Only an IN_PROGRESS ticket can be transferred (current status: " + ticket.getStatus() + ")");
        }
        Employee target = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + targetEmployeeId));
        if (!teamRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), targetEmployeeId)) {
            throw new BusinessException("Can only transfer to a member of this department's ticket team");
        }

        Employee previousAssignee = ticket.getAssignedTo();
        ticket.setAssignedTo(target);
        ticket.setStatus("OPEN"); // new assignee must claim it fresh - resets the acceptance clock
        ticket.setAssignedAt(LocalDateTime.now());
        ticket.setAcceptedAt(null);
        ticket.setAcceptanceBreached(false);
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(previousAssignee).entryType("TRANSFER").message(message).targetEmployee(target).build());
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO acceptResolution(Long ticketId, Long employeeId) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireRaiser(ticket, employeeId);
        if (!List.of("RESOLVED", "REJECTED").contains(ticket.getStatus())) {
            throw new BusinessException("Only a RESOLVED or REJECTED ticket can be accepted (current status: " + ticket.getStatus() + ")");
        }
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(ticket.getRaisedBy()).entryType("ACCEPT").build());
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO escalate(Long ticketId, Long employeeId, String message) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireRaiser(ticket, employeeId);
        if (!List.of("RESOLVED", "REJECTED").contains(ticket.getStatus())) {
            throw new BusinessException("Only a RESOLVED or REJECTED ticket can be escalated (current status: " + ticket.getStatus() + ")");
        }

        Department department = ticket.getDepartment();
        Employee hod = department.getHeadOfDepartment();
        Employee currentAssignee = ticket.getAssignedTo();

        // Escalation ceiling: once it's already sitting with the HOD, there's
        // nowhere further to send it - fail loudly instead of silently
        // reassigning it back to the same person.
        if (hod != null && currentAssignee != null && hod.getId().equals(currentAssignee.getId())) {
            throw new BusinessException("This ticket has already been escalated to the Head of Department - no further escalation is available");
        }

        List<DepartmentTicketTeam> escalationPool = teamRepository.findByDepartmentIdAndRoleInTeam(department.getId(), "ESCALATION");
        Employee target = escalationPool.stream()
                .map(DepartmentTicketTeam::getEmployee)
                .filter(e -> currentAssignee == null || !e.getId().equals(currentAssignee.getId()))
                .findFirst()
                .orElse(hod);

        if (target == null) {
            throw new BusinessException("No escalation contact or Head of Department is configured for this department");
        }

        ticket.setAssignedTo(target);
        ticket.setStatus("OPEN");
        ticket.setEscalationLevel(ticket.getEscalationLevel() + 1);
        ticket.setAssignedAt(LocalDateTime.now());
        ticket.setAcceptedAt(null);
        ticket.setResolvedAt(null);
        ticket.setAcceptanceBreached(false);
        ticket.setResolutionBreached(false);
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(ticket.getRaisedBy()).entryType("ESCALATE").message(message).targetEmployee(target).build());
        auditService.log("ESCALATE", "Ticket", ticket.getId(), ticket.getTicketNumber() + " escalated to " + target.getFullName());
        return ticketMapper.toDTO(ticket, false);
    }

    @Override
    public TicketDTO closeTicket(Long ticketId, Long employeeId, String message) {
        TicketMaster ticket = getTicketOrThrow(ticketId);
        requireRaiser(ticket, employeeId);
        if (!List.of("OPEN", "IN_PROGRESS").contains(ticket.getStatus())) {
            throw new BusinessException("This ticket already has an outcome - use Accept instead of Close (current status: " + ticket.getStatus() + ")");
        }
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        conversationRepository.save(TicketConversation.builder()
                .ticket(ticket).author(ticket.getRaisedBy()).entryType("CLOSE").message(message).build());
        return ticketMapper.toDTO(ticket, false);
    }

    // ============================================================
    // SLA breach scanning - called by TicketSlaScheduler on a timer.
    // Records breaches (flags + a system conversation entry); does NOT
    // auto-escalate. Whether to auto-escalate on breach is a genuinely
    // separate decision left to a human for now - see chat discussion.
    // ============================================================

    @Override
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();

        for (TicketMaster ticket : ticketRepository.findOpenUnbreachedForAcceptanceCheck()) {
            int[] hours = resolveSlaHours(ticket);
            if (ticket.getAssignedAt().plusHours(hours[0]).isBefore(now)) {
                ticket.setAcceptanceBreached(true);
                ticketRepository.save(ticket);
                conversationRepository.save(TicketConversation.builder()
                        .ticket(ticket).entryType("SLA_BREACH")
                        .message("Acceptance SLA breached - not claimed within " + hours[0] + "h of assignment")
                        .build());
                log.warn("Ticket {} breached acceptance SLA ({}h)", ticket.getTicketNumber(), hours[0]);
            }
        }

        for (TicketMaster ticket : ticketRepository.findInProgressForResolutionCheck()) {
            int[] hours = resolveSlaHours(ticket);
            if (ticket.getAcceptedAt().plusHours(hours[1]).isBefore(now)) {
                ticket.setResolutionBreached(true);
                ticketRepository.save(ticket);
                conversationRepository.save(TicketConversation.builder()
                        .ticket(ticket).entryType("SLA_BREACH")
                        .message("Resolution SLA breached - not resolved within " + hours[1] + "h of acceptance")
                        .build());
                log.warn("Ticket {} breached resolution SLA ({}h)", ticket.getTicketNumber(), hours[1]);
            }
        }
    }

    // ============================================================
    // Department ticket team (admin config)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentTicketTeamDTO> getTeam(Long departmentId) {
        return teamRepository.findByDepartmentId(departmentId).stream()
                .map(t -> DepartmentTicketTeamDTO.builder()
                        .id(t.getId())
                        .departmentId(t.getDepartment().getId())
                        .employeeId(t.getEmployee().getId())
                        .employeeName(t.getEmployee().getFullName())
                        .roleInTeam(t.getRoleInTeam())
                        .build())
                .toList();
    }

    @Override
    public DepartmentTicketTeamDTO addTeamMember(DepartmentTicketTeamDTO dto) {
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + dto.getEmployeeId()));
        String role = dto.getRoleInTeam() == null ? "" : dto.getRoleInTeam().toUpperCase();
        if (!List.of("MEMBER", "ESCALATION").contains(role)) {
            throw new BusinessException("roleInTeam must be MEMBER or ESCALATION");
        }
        if (teamRepository.existsByDepartmentIdAndEmployeeIdAndRoleInTeam(department.getId(), employee.getId(), role)) {
            throw new BusinessException(employee.getFullName() + " is already a " + role + " for this department");
        }

        DepartmentTicketTeam saved = teamRepository.save(DepartmentTicketTeam.builder()
                .department(department).employee(employee).roleInTeam(role).build());
        return DepartmentTicketTeamDTO.builder()
                .id(saved.getId()).departmentId(department.getId())
                .employeeId(employee.getId()).employeeName(employee.getFullName())
                .roleInTeam(role).build();
    }

    @Override
    public void removeTeamMember(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team membership not found: " + id);
        }
        teamRepository.deleteById(id);
    }

    // ============================================================
    // SLA policy (admin config)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyDTO> getSlaPolicies(Long departmentId) {
        return slaPolicyRepository.findByDepartmentId(departmentId).stream()
                .map(p -> SlaPolicyDTO.builder()
                        .id(p.getId()).departmentId(p.getDepartment().getId())
                        .priority(p.getPriority())
                        .acceptanceHours(p.getAcceptanceHours())
                        .resolutionHours(p.getResolutionHours())
                        .build())
                .toList();
    }

    @Override
    public SlaPolicyDTO saveSlaPolicy(SlaPolicyDTO dto) {
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));
        String priority = normalizePriority(dto.getPriority());

        SlaPolicy policy = slaPolicyRepository.findByDepartmentIdAndPriority(department.getId(), priority)
                .orElseGet(() -> SlaPolicy.builder().department(department).priority(priority).build());
        policy.setAcceptanceHours(dto.getAcceptanceHours());
        policy.setResolutionHours(dto.getResolutionHours());
        SlaPolicy saved = slaPolicyRepository.save(policy);

        return SlaPolicyDTO.builder()
                .id(saved.getId()).departmentId(department.getId()).priority(priority)
                .acceptanceHours(saved.getAcceptanceHours()).resolutionHours(saved.getResolutionHours())
                .build();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private TicketMaster getTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    private String normalizePriority(String priority) {
        String p = (priority == null || priority.isBlank()) ? "MEDIUM" : priority.toUpperCase();
        if (!DEFAULT_SLA_HOURS.containsKey(p)) {
            throw new BusinessException("priority must be LOW, MEDIUM, or HIGH");
        }
        return p;
    }

    private int[] resolveSlaHours(TicketMaster ticket) {
        return slaPolicyRepository.findByDepartmentIdAndPriority(ticket.getDepartment().getId(), ticket.getPriority())
                .map(p -> new int[]{p.getAcceptanceHours(), p.getResolutionHours()})
                .orElseGet(() -> DEFAULT_SLA_HOURS.get(ticket.getPriority()));
    }

    private void requireAssignee(TicketMaster ticket, Long employeeId) {
        if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(employeeId)) {
            throw new BusinessException("Only the employee this ticket is currently assigned to can do that");
        }
    }

    private void requireRaiser(TicketMaster ticket, Long employeeId) {
        if (!ticket.getRaisedBy().getId().equals(employeeId)) {
            throw new BusinessException("Only the employee who raised this ticket can do that");
        }
    }

    // Reply is allowed by either the raiser or the current assignee.
    private void requireInvolved(TicketMaster ticket, Long employeeId) {
        boolean isRaiser = ticket.getRaisedBy().getId().equals(employeeId);
        boolean isAssignee = ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(employeeId);
        if (!isRaiser && !isAssignee) {
            throw new BusinessException("Only the ticket's raiser or currently assigned handler can reply");
        }
    }

    // View access: raiser, current assignee, any team member (MEMBER or
    // ESCALATION) of the ticket's department, the department's HOD, or an
    // ADMIN/MANAGER (privileged=true, checked by the caller/controller).
    private void authorizeView(TicketMaster ticket, Long requesterEmployeeId, boolean privileged) {
        if (privileged) return;
        if (requesterEmployeeId == null) {
            throw new BusinessException("Not authorized to view this ticket");
        }
        boolean isRaiser = ticket.getRaisedBy().getId().equals(requesterEmployeeId);
        boolean isAssignee = ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(requesterEmployeeId);
        boolean isHod = ticket.getDepartment().getHeadOfDepartment() != null
                && ticket.getDepartment().getHeadOfDepartment().getId().equals(requesterEmployeeId);
        boolean isTeamMember = teamRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), requesterEmployeeId);
        if (!isRaiser && !isAssignee && !isHod && !isTeamMember) {
            throw new BusinessException("Not authorized to view this ticket");
        }
    }

    private boolean hasUnread(TicketMaster ticket, Long employeeId) {
        if (employeeId == null) return false;
        return conversationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticket.getId())
                .map(latest -> readStatusRepository.findByTicketIdAndEmployeeId(ticket.getId(), employeeId)
                        .map(rs -> latest.getCreatedAt().isAfter(rs.getLastReadAt()))
                        .orElse(true)) // never read at all -> unread if there's any activity
                .orElse(false); // no conversation entries yet at all
    }

    private PageResponse<TicketDTO> toPageResponse(Page<TicketMaster> page, Long requesterEmployeeId) {
        return PageResponse.<TicketDTO>builder()
                .content(page.getContent().stream().map(t -> ticketMapper.toDTO(t, hasUnread(t, requesterEmployeeId))).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
