package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.ResignationAssetDTO;
import com.enterprise.ems.dto.ResignationDTO;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.ResignationMapper;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.ResignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
 * ================================================================================
 * RESIGNATION / SEPARATION MODULE - state machine reference
 * ================================================================================
 *   SUBMITTED -> (each WorkflowStage cleared in sequenceOrder) ->
 *   IN_APPROVAL -> ALL_APPROVALS_DONE -> (clearance rows created automatically) ->
 *   CLEARANCE_PENDING -> (every department CLEARED) -> CLEARANCE_DONE ->
 *   finalApprove() -> FINAL_APPROVED (terminal - employee deactivated, login disabled)
 *
 *   Any approval stage REJECTED -> resignation REJECTED (terminal)
 *   Employee withdraw() at any point before FINAL_APPROVED -> WITHDRAWN (terminal)
 *
 * APPROVER RESOLUTION happens ONCE, at submit() time, and is then frozen onto
 * each ResignationApproval row (see resolveApprover()). If the resolved
 * manager/role-holder changes later, history still correctly shows who was
 * actually asked - same principle as TicketMaster freezing assignedTo.
 *
 * NO-DUES CLEARANCE is deliberately separate from the approval stages: it's
 * "every active department must independently clear", not one more
 * sequential approval - see ResignationClearance entity javadoc.
 * ================================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ResignationServiceImpl implements ResignationService {

    // Ticket statuses that count as "still holding open work" for the final
    // approval gate (see finalApprove()).
    private static final List<String> OPEN_TICKET_STATUSES = List.of("OPEN", "IN_PROGRESS");

    private static final List<String> TERMINAL_RESIGNATION_STATUSES =
            List.of("FINAL_APPROVED", "REJECTED", "WITHDRAWN");

    private final ResignationRepository resignationRepository;
    private final ResignationApprovalRepository approvalRepository;
    private final WorkflowStageRepository stageRepository;
    private final ResignationClearanceRepository clearanceRepository;
    private final ResignationAssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketMasterRepository ticketRepository;
    private final LeaveRepository leaveRepository;
    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;
    private final ResignationMapper resignationMapper;
    private final AuditService auditService;

    // ============================================================
    // Submit / withdraw
    // ============================================================

    @Override
    public ResignationDTO submit(Long employeeId, String reason, Integer noticePeriodDaysOverride) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        if (!resignationRepository.findByEmployeeIdAndStatusNotIn(employeeId, TERMINAL_RESIGNATION_STATUSES).isEmpty()) {
            throw new BusinessException("You already have a resignation in progress - withdraw it first if you want to submit a new one");
        }

        List<WorkflowStage> stages = stageRepository
                .findByPrivilegeModule_CodeAndActiveTrueOrderBySequenceOrderAsc(AppConstants.MODULE_CODE_RESIGNATION);
        if (stages.isEmpty()) {
            throw new BusinessException("No approval chain is configured for resignations - contact HR/Admin to set one up");
        }

        int noticePeriodDays = noticePeriodDaysOverride != null && noticePeriodDaysOverride > 0
                ? noticePeriodDaysOverride
                : AppConstants.DEFAULT_NOTICE_PERIOD_DAYS;

        LocalDate requestedOn = LocalDate.now();

        Resignation resignation = Resignation.builder()
                .employee(employee)
                .reason(reason)
                .requestedOn(requestedOn)
                .noticePeriodDays(noticePeriodDays)
                .expectedLastWorkingDay(requestedOn.plusDays(noticePeriodDays))
                .status(AppConstants.RESIGNATION_SUBMITTED)
                .build();
        resignation = resignationRepository.save(resignation);

        for (WorkflowStage stage : stages) {
            Employee approver = resolveApprover(stage, employee);
            String status = approver != null ? AppConstants.RESIGNATION_APPROVAL_PENDING : AppConstants.RESIGNATION_APPROVAL_SKIPPED;
            String remarks = approver != null ? null : "Auto-skipped: no eligible approver could be resolved for this stage";
            approvalRepository.save(ResignationApproval.builder()
                    .resignation(resignation)
                    .stage(stage)
                    .stageName(stage.getName())
                    .sequenceOrder(stage.getSequenceOrder())
                    .approverEmployee(approver != null ? approver : employee) // never null in DB - see note below
                    .status(status)
                    .remarks(remarks)
                    .actedAt(approver == null ? LocalDateTime.now() : null)
                    .build());
        }
        // Note: approverEmployee is NOT NULL in the schema, so a SKIPPED stage
        // with no resolvable approver is attributed to the resigning employee
        // themselves as a harmless placeholder - the SKIPPED status plus the
        // remarks text is what the UI/admin should actually read, not this FK.

        resignation.setStatus(AppConstants.RESIGNATION_IN_APPROVAL);
        advanceIfCurrentStagesAllSkipped(resignation);
        resignationRepository.save(resignation);

        auditService.log("CREATE", "Resignation", resignation.getId(),
                "Resignation submitted by " + employee.getFullName() + ", expected LWD " + resignation.getExpectedLastWorkingDay());
        return resignationMapper.toDTO(resignation);
    }

    @Override
    @Transactional(readOnly = true)
    public ResignationDTO getMyLatest(Long employeeId) {
        return resignationRepository.findTopByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .map(resignationMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("No resignation found for this employee"));
    }

    @Override
    public ResignationDTO withdraw(Long resignationId, Long employeeId) {
        Resignation resignation = getOrThrow(resignationId);
        if (!resignation.getEmployee().getId().equals(employeeId)) {
            throw new BusinessException("Only the employee who submitted this resignation can withdraw it");
        }
        if (TERMINAL_RESIGNATION_STATUSES.contains(resignation.getStatus())) {
            throw new BusinessException("This resignation already has a final outcome (" + resignation.getStatus() + ") and can't be withdrawn");
        }
        resignation.setStatus(AppConstants.RESIGNATION_WITHDRAWN);
        resignationRepository.save(resignation);
        auditService.log("WITHDRAW", "Resignation", resignation.getId(), "Withdrawn by " + resignation.getEmployee().getFullName());
        return resignationMapper.toDTO(resignation);
    }

    // ============================================================
    // Viewing
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ResignationDTO getById(Long resignationId, Long requesterEmployeeId, boolean privileged) {
        Resignation resignation = getOrThrow(resignationId);
        if (!privileged) {
            boolean isOwner = resignation.getEmployee().getId().equals(requesterEmployeeId);
            boolean isApprover = resignation.getApprovals().stream()
                    .anyMatch(a -> a.getApproverEmployee().getId().equals(requesterEmployeeId));
            if (!isOwner && !isApprover) {
                throw new BusinessException("Not authorized to view this resignation");
            }
        }
        return resignationMapper.toDTO(resignation);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResignationDTO> search(String status, Pageable pageable) {
        Page<Resignation> page = (status == null || status.isBlank())
                ? resignationRepository.findAll(pageable)
                : resignationRepository.findByStatus(status, pageable);
        return PageResponse.<ResignationDTO>builder()
                .content(page.getContent().stream().map(resignationMapper::toDTO).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ============================================================
    // Approval stages
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResignationDTO> getPendingApprovalsForMe(Long approverEmployeeId) {
        return approvalRepository.findByApproverEmployeeIdAndStatus(approverEmployeeId, AppConstants.RESIGNATION_APPROVAL_PENDING)
                .stream()
                .map(a -> resignationMapper.toDTO(a.getResignation()))
                .distinct()
                .toList();
    }

    @Override
    public ResignationDTO approveStage(Long resignationId, Long approverEmployeeId, String remarks) {
        ResignationApproval current = currentActionableStageOrThrow(resignationId, approverEmployeeId);
        current.setStatus(AppConstants.RESIGNATION_APPROVAL_APPROVED);
        current.setRemarks(remarks);
        current.setActedAt(LocalDateTime.now());
        approvalRepository.save(current);

        Resignation resignation = current.getResignation();
        auditService.log("APPROVE", "Resignation", resignation.getId(),
                current.getStageName() + " approved by " + current.getApproverEmployee().getFullName());

        boolean allDone = approvalRepository.findByResignationIdOrderBySequenceOrderAsc(resignation.getId()).stream()
                .allMatch(a -> List.of(AppConstants.RESIGNATION_APPROVAL_APPROVED, AppConstants.RESIGNATION_APPROVAL_SKIPPED).contains(a.getStatus()));

        if (allDone) {
            resignation.setStatus(AppConstants.RESIGNATION_ALL_APPROVALS_DONE);
            enterClearance(resignation);
        }
        resignationRepository.save(resignation);
        return resignationMapper.toDTO(resignation);
    }

    @Override
    public ResignationDTO rejectStage(Long resignationId, Long approverEmployeeId, String remarks) {
        if (remarks == null || remarks.isBlank()) {
            throw new BusinessException("A reason is required to reject a resignation");
        }
        ResignationApproval current = currentActionableStageOrThrow(resignationId, approverEmployeeId);
        current.setStatus(AppConstants.RESIGNATION_APPROVAL_REJECTED);
        current.setRemarks(remarks);
        current.setActedAt(LocalDateTime.now());
        approvalRepository.save(current);

        Resignation resignation = current.getResignation();
        resignation.setStatus(AppConstants.RESIGNATION_REJECTED);
        resignationRepository.save(resignation);

        auditService.log("REJECT", "Resignation", resignation.getId(),
                current.getStageName() + " rejected by " + current.getApproverEmployee().getFullName() + ": " + remarks);
        return resignationMapper.toDTO(resignation);
    }

    // ============================================================
    // No-dues clearance
    // ============================================================

    @Override
    public ResignationDTO clearDepartment(Long resignationId, Long departmentId, Long actorEmployeeId, String remarks) {
        Resignation resignation = getOrThrow(resignationId);
        requireStatus(resignation, AppConstants.RESIGNATION_CLEARANCE_PENDING);
        ResignationClearance clearance = clearanceOrThrow(resignation, departmentId);
        Employee actor = employeeRepository.findById(actorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + actorEmployeeId));

        clearance.setStatus(AppConstants.CLEARANCE_CLEARED);
        clearance.setClearedBy(actor);
        clearance.setRemarks(remarks);
        clearance.setClearedAt(LocalDateTime.now());
        clearanceRepository.save(clearance);

        boolean allCleared = clearanceRepository.findByResignationId(resignation.getId()).stream()
                .allMatch(c -> AppConstants.CLEARANCE_CLEARED.equals(c.getStatus()));
        if (allCleared) {
            resignation.setStatus(AppConstants.RESIGNATION_CLEARANCE_DONE);
            resignationRepository.save(resignation);
        }
        auditService.log("CLEAR", "Resignation", resignation.getId(),
                clearance.getDepartment().getName() + " cleared by " + actor.getFullName());
        return resignationMapper.toDTO(resignation);
    }

    @Override
    public ResignationDTO blockDepartment(Long resignationId, Long departmentId, Long actorEmployeeId, String remarks) {
        if (remarks == null || remarks.isBlank()) {
            throw new BusinessException("A reason is required to block clearance (e.g. asset not yet returned)");
        }
        Resignation resignation = getOrThrow(resignationId);
        requireStatus(resignation, AppConstants.RESIGNATION_CLEARANCE_PENDING);
        ResignationClearance clearance = clearanceOrThrow(resignation, departmentId);
        Employee actor = employeeRepository.findById(actorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + actorEmployeeId));

        clearance.setStatus(AppConstants.CLEARANCE_BLOCKED);
        clearance.setClearedBy(actor);
        clearance.setRemarks(remarks);
        clearanceRepository.save(clearance);
        return resignationMapper.toDTO(resignation);
    }

    // ============================================================
    // Asset return line items
    // ============================================================

    @Override
    public ResignationAssetDTO addAsset(Long resignationId, String assetDescription) {
        Resignation resignation = getOrThrow(resignationId);
        if (assetDescription == null || assetDescription.isBlank()) {
            throw new BusinessException("Asset description is required");
        }
        ResignationAsset asset = assetRepository.save(ResignationAsset.builder()
                .resignation(resignation)
                .assetDescription(assetDescription)
                .returned(false)
                .build());
        return resignationMapper.toDTO(asset);
    }

    @Override
    public ResignationDTO markAssetReturned(Long resignationId, Long assetId, String remarks) {
        Resignation resignation = getOrThrow(resignationId);
        ResignationAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset item not found: " + assetId));
        if (!asset.getResignation().getId().equals(resignationId)) {
            throw new BusinessException("This asset item does not belong to this resignation");
        }
        asset.setReturned(true);
        asset.setRemarks(remarks);
        asset.setReturnedAt(LocalDateTime.now());
        assetRepository.save(asset);
        return resignationMapper.toDTO(resignation);
    }

    // ============================================================
    // Final approval
    // ============================================================

    @Override
    public ResignationDTO finalApprove(Long resignationId, Long approverEmployeeId) {
        Resignation resignation = getOrThrow(resignationId);
        requireStatus(resignation, AppConstants.RESIGNATION_CLEARANCE_DONE);

        Employee employee = resignation.getEmployee();

        long unreturnedAssets = assetRepository.countByResignationIdAndReturnedFalse(resignation.getId());
        if (unreturnedAssets > 0) {
            throw new BusinessException(unreturnedAssets + " asset item(s) are still marked as not returned - resolve those before final approval");
        }

        // "Any pending request" check (point 6 of the original requirement).
        // Enumerated explicitly rather than left vague: open/in-progress
        // tickets currently assigned to this employee, and any leave request
        // still awaiting approval. Extend this list here if more work-item
        // types should block final approval later.
        long openTickets = ticketRepository.countByAssignedToIdAndStatusIn(employee.getId(), OPEN_TICKET_STATUSES);
        if (openTickets > 0) {
            throw new BusinessException("This employee still has " + openTickets +
                    " open/in-progress ticket(s) assigned to them. Transfer or resolve those first (Tickets > Transfer), then retry final approval.");
        }
        long pendingLeaves = leaveRepository.countByEmployeeIdAndStatus(employee.getId(), "PENDING");
        if (pendingLeaves > 0) {
            throw new BusinessException("This employee still has " + pendingLeaves +
                    " pending leave request(s). Approve/reject those first, then retry final approval.");
        }

        resignation.setStatus(AppConstants.RESIGNATION_FINAL_APPROVED);
        if (resignation.getActualLastWorkingDay() == null) {
            resignation.setActualLastWorkingDay(resignation.getExpectedLastWorkingDay());
        }
        resignationRepository.save(resignation);

        employee.setActive(false);
        if (employee.getUser() != null) {
            employee.getUser().setEnabled(false);
        }
        employeeRepository.save(employee);

        Employee approver = employeeRepository.findById(approverEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + approverEmployeeId));
        auditService.log("FINAL_APPROVE", "Resignation", resignation.getId(),
                "Finally approved by " + approver.getFullName() + " - " + employee.getFullName() +
                        " relieved effective " + resignation.getActualLastWorkingDay());
        return resignationMapper.toDTO(resignation);
    }

    @Override
    public ResignationDTO overrideLastWorkingDay(Long resignationId, LocalDate actualLastWorkingDay, String overrideReason) {
        Resignation resignation = getOrThrow(resignationId);
        if (TERMINAL_RESIGNATION_STATUSES.contains(resignation.getStatus())) {
            throw new BusinessException("This resignation already has a final outcome and its last working day can't be changed");
        }
        if (overrideReason == null || overrideReason.isBlank()) {
            throw new BusinessException("A reason is required when overriding the last working day");
        }
        resignation.setActualLastWorkingDay(actualLastWorkingDay);
        resignation.setOverrideReason(overrideReason);
        resignationRepository.save(resignation);
        return resignationMapper.toDTO(resignation);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Resignation getOrThrow(Long resignationId) {
        return resignationRepository.findById(resignationId)
                .orElseThrow(() -> new ResourceNotFoundException("Resignation not found: " + resignationId));
    }

    private void requireStatus(Resignation resignation, String expected) {
        if (!expected.equals(resignation.getStatus())) {
            throw new BusinessException("This action requires status " + expected + " (current status: " + resignation.getStatus() + ")");
        }
    }

    // The stage this approver is allowed to act on RIGHT NOW: the lowest
    // sequenceOrder row still PENDING. Enforces "stage N+1 can't be actioned
    // before stage N" purely from data, no separate lock/flag needed.
    private ResignationApproval currentActionableStageOrThrow(Long resignationId, Long approverEmployeeId) {
        ResignationApproval current = approvalRepository
                .findFirstByResignationIdAndStatusOrderBySequenceOrderAsc(resignationId, AppConstants.RESIGNATION_APPROVAL_PENDING)
                .orElseThrow(() -> new BusinessException("There is no pending approval stage on this resignation right now"));
        if (!current.getApproverEmployee().getId().equals(approverEmployeeId)) {
            throw new BusinessException("You are not the approver for the current stage (" + current.getStageName() + ") of this resignation");
        }
        return current;
    }

    // Edge case: if every configured stage resolved to "no eligible
    // approver" (SKIPPED), there's nothing left pending - jump straight to
    // clearance instead of leaving the resignation silently stuck in
    // IN_APPROVAL forever.
    private void advanceIfCurrentStagesAllSkipped(Resignation resignation) {
        boolean anyPending = resignation.getApprovals().stream()
                .anyMatch(a -> AppConstants.RESIGNATION_APPROVAL_PENDING.equals(a.getStatus()));
        if (!anyPending) {
            resignation.setStatus(AppConstants.RESIGNATION_ALL_APPROVALS_DONE);
            enterClearance(resignation);
        }
    }

    // Creates one PENDING ResignationClearance row per active department.
    // v1 simplification: EVERY active department clears, not a
    // policy-selected subset - see chat discussion on resignation_clearance.
    // Cheap to narrow later (e.g. a `requires_resignation_clearance` flag on
    // Department) without touching anything else in this class.
    private void enterClearance(Resignation resignation) {
        List<Department> departments = departmentRepository.findByActiveTrue();
        if (departments.isEmpty()) {
            resignation.setStatus(AppConstants.RESIGNATION_CLEARANCE_DONE);
            return;
        }
        for (Department department : departments) {
            clearanceRepository.save(ResignationClearance.builder()
                    .resignation(resignation)
                    .department(department)
                    .status(AppConstants.CLEARANCE_PENDING)
                    .build());
        }
        resignation.setStatus(AppConstants.RESIGNATION_CLEARANCE_PENDING);
    }

    private ResignationClearance clearanceOrThrow(Resignation resignation, Long departmentId) {
        return clearanceRepository.findByResignationId(resignation.getId()).stream()
                .filter(c -> c.getDepartment().getId().equals(departmentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No clearance requirement for department " + departmentId + " on this resignation"));
    }

    // Resolves the actual approver Employee for a stage, per its
    // approverType - see WorkflowStage entity javadoc. Returns
    // null (not throws) when nobody eligible is found, so submit() can
    // record a SKIPPED stage instead of blocking the whole resignation on a
    // misconfigured/incomplete policy.
    //
    // ROLE_IN_DEPARTMENT / SPECIFIC_ROLE resolve against EmployeeWorkflowRole
    // (admin-managed "Employee Access" assignments), NOT the auth Role/User
    // relationship - business approval routing is deliberately decoupled
    // from Spring Security login permissions. See WorkflowRole /
    // EmployeeWorkflowRole entity javadocs.
    private Employee resolveApprover(WorkflowStage stage, Employee resigningEmployee) {
        return switch (stage.getApproverType()) {
            case AppConstants.APPROVER_TYPE_REPORTING_MANAGER -> resigningEmployee.getReportingManager();

            case AppConstants.APPROVER_TYPE_ROLE_IN_DEPARTMENT -> {
                if (stage.getWorkflowRole() == null) yield null;
                Long departmentId = "FIXED_DEPARTMENT".equals(stage.getDepartmentScope()) && stage.getFixedDepartment() != null
                        ? stage.getFixedDepartment().getId()
                        : (resigningEmployee.getDepartment() != null ? resigningEmployee.getDepartment().getId() : null);
                if (departmentId == null) yield null;
                List<EmployeeWorkflowRole> candidates = employeeWorkflowRoleRepository
                        .findByWorkflowRoleIdAndScopeDepartmentIdAndActiveTrueOrderByIdAsc(stage.getWorkflowRole().getId(), departmentId);
                yield candidates.stream()
                        .map(EmployeeWorkflowRole::getEmployee)
                        .filter(e -> !e.getId().equals(resigningEmployee.getId())) // don't let someone approve their own resignation
                        .findFirst()
                        .orElse(null);
            }

            case AppConstants.APPROVER_TYPE_SPECIFIC_ROLE -> {
                if (stage.getWorkflowRole() == null) yield null;
                List<EmployeeWorkflowRole> candidates = employeeWorkflowRoleRepository
                        .findByWorkflowRoleIdAndScopeDepartmentIsNullAndActiveTrueOrderByIdAsc(stage.getWorkflowRole().getId());
                yield candidates.stream()
                        .map(EmployeeWorkflowRole::getEmployee)
                        .filter(e -> !e.getId().equals(resigningEmployee.getId()))
                        .findFirst()
                        .orElse(null);
            }

            case AppConstants.APPROVER_TYPE_SPECIFIC_USER -> stage.getSpecificEmployee();

            default -> null;
        };
    }
}
