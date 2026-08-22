package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.security.CurrentEmployeeResolver;
import com.enterprise.ems.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: Ticketing module REST API.
 * See TicketServiceImpl for the full state machine and SLA design.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketApiController {

    private final TicketService ticketService;
    private final CurrentEmployeeResolver currentEmployeeResolver;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketDTO>> create(
            @Valid @RequestBody TicketDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket raised", ticketService.createTicket(dto, employee.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDTO>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long employeeId = resolveOptionalEmployeeId(principal);
        boolean privileged = currentEmployeeResolver.isPrivileged(principal);
        return ResponseEntity.ok(ApiResponse.success(ticketService.getById(id, employeeId, privileged)));
    }

    @GetMapping("/{id}/conversation")
    public ResponseEntity<ApiResponse<List<TicketConversationDTO>>> getConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long employeeId = resolveOptionalEmployeeId(principal);
        boolean privileged = currentEmployeeResolver.isPrivileged(principal);
        return ResponseEntity.ok(ApiResponse.success(ticketService.getConversation(id, employeeId, privileged)));
    }

    // Self-service: tickets I raised.
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<TicketDTO>>> getMyTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getMyTickets(employee.getId(), status, priority, PageRequest.of(page, size))));
    }

    // Self-service: tickets currently assigned to me (I'm on some department's ticket team).
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<PageResponse<TicketDTO>>> getAssignedTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getAssignedTickets(employee.getId(), status, priority, PageRequest.of(page, size))));
    }

    // Self-service: the unclaimed pool for departments I'm on the ticket
    // team for (OPEN + unassigned). Open to any authenticated employee -
    // team membership itself is the authorization check, done inside the
    // service via DepartmentTicketTeamRepository, so no @PreAuthorize here.
    @GetMapping("/claimable")
    public ResponseEntity<ApiResponse<PageResponse<TicketDTO>>> getClaimableTickets(
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getClaimableTickets(employee.getId(), priority, PageRequest.of(page, size))));
    }

    // Admin/manager: every ticket, any department, filterable.
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<TicketDTO>>> search(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.searchTickets(departmentId, status, priority, keyword, PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        ticketService.markRead(id, employee.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record ReplyRequest(String message, Long parentEntryId) {}

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<TicketConversationDTO>> reply(
            @PathVariable Long id, @RequestBody ReplyRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.addReply(id, employee.getId(), body.message(), body.parentEntryId())));
    }

    // --- Responsible-person actions ---

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<TicketDTO>> claim(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Ticket claimed", ticketService.claim(id, employee.getId())));
    }

    public record ActionRequest(String message) {}

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<TicketDTO>> resolve(
            @PathVariable Long id, @RequestBody(required = false) ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        String message = body != null ? body.message() : null;
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", ticketService.resolve(id, employee.getId(), message)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TicketDTO>> reject(
            @PathVariable Long id, @RequestBody ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Ticket rejected", ticketService.reject(id, employee.getId(), body.message())));
    }

    public record TransferRequest(Long targetEmployeeId, String message) {}

    @PostMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<TicketDTO>> transfer(
            @PathVariable Long id, @RequestBody TransferRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Ticket transferred",
                ticketService.transfer(id, employee.getId(), body.targetEmployeeId(), body.message())));
    }

    // --- Raiser (user-side) actions ---

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<TicketDTO>> accept(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Outcome accepted - ticket closed", ticketService.acceptResolution(id, employee.getId())));
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<ApiResponse<TicketDTO>> escalate(
            @PathVariable Long id, @RequestBody(required = false) ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        String message = body != null ? body.message() : null;
        return ResponseEntity.ok(ApiResponse.success("Ticket escalated", ticketService.escalate(id, employee.getId(), message)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<TicketDTO>> close(
            @PathVariable Long id, @RequestBody(required = false) ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        String message = body != null ? body.message() : null;
        return ResponseEntity.ok(ApiResponse.success("Ticket closed", ticketService.closeTicket(id, employee.getId(), message)));
    }

    private Long resolveOptionalEmployeeId(UserDetails principal) {
        try {
            return currentEmployeeResolver.requireCurrentEmployee(principal).getId();
        } catch (RuntimeException e) {
            // A pure ADMIN/IT login with no linked Employee record - fine, they
            // still get through on the privileged check inside the service.
            return null;
        }
    }
}

@RestController
@RequestMapping("/api/ticket-teams")
@RequiredArgsConstructor
class TicketTeamApiController {

    private final TicketService ticketService;

    // Readable by any authenticated employee - a ticket handler needs this
    // list to pick a transfer target, not just admins configuring the team.
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentTicketTeamDTO>>> getTeam(@RequestParam Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTeam(departmentId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentTicketTeamDTO>> add(@Valid @RequestBody DepartmentTicketTeamDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Added to team", ticketService.addTeamMember(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        ticketService.removeTeamMember(id);
        return ResponseEntity.ok(ApiResponse.success("Removed from team", null));
    }
}

@RestController
@RequestMapping("/api/sla-policies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class SlaPolicyApiController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlaPolicyDTO>>> get(@RequestParam Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getSlaPolicies(departmentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SlaPolicyDTO>> save(@Valid @RequestBody SlaPolicyDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("SLA policy saved", ticketService.saveSlaPolicy(dto)));
    }
}
