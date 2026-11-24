package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.constant.ApprovalStatus;
import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.dto.HiringRequests.RequisitionAction;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.RequisitionDTO;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.HiringMapper;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.RequisitionService;
import com.enterprise.ems.service.hiring.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/*
 * PURPOSE: Job requisition lifecycle - create, edit, submit for approval (using the
 * admin-configured REQUISITION chain), approve / reject, and open / hold / close.
 * Modelled on ResignationServiceImpl so the approval behaviour is identical.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequisitionServiceImpl implements RequisitionService {

    private final JobRequisitionRepository requisitionRepository;
    private final JobApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeTypeRepository employeeTypeRepository;
    private final LocationRepository locationRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final WorkflowApproverResolver approverResolver;
    private final HiringAccessService access;
    private final HiringMapper mapper;
    private final HiringNotifier notifier;
    private final AuditService auditService;

    // ============================================================ create / edit

    @Override
    public RequisitionDTO create(RequisitionDTO dto, HiringActor actor) {
        access.requireCanRaiseRequisition(actor);

        JobRequisition r = JobRequisition.builder()
                .requestedBy(actor.employee())
                .status(RequisitionStatus.DRAFT)
                .filledCount(0)
                .build();
        applyFields(r, dto, actor);
        r = requisitionRepository.save(r);

        // Human-friendly code, possible only once the id exists.
        r.setRequisitionCode(String.format("REQ-%d-%04d", LocalDate.now().getYear(), r.getId()));
        r = requisitionRepository.save(r);

        auditService.log("CREATE", "JobRequisition", r.getId(), "Created requisition " + r.getRequisitionCode() + " - " + r.getTitle());
        return toDTO(r, actor);
    }

    @Override
    public RequisitionDTO update(Long id, RequisitionDTO dto, HiringActor actor) {
        JobRequisition r = getOrThrow(id);
        if (!(actor.staff() || r.getRequestedBy().getId().equals(actor.id()))) {
            throw new AccessDeniedException("Only the person who raised this requisition, a recruiter or an admin can edit it");
        }
        if (r.getStatus() != RequisitionStatus.DRAFT && r.getStatus() != RequisitionStatus.REJECTED) {
            throw new BusinessException("Only DRAFT or REJECTED requisitions can be edited (current status: " + r.getStatus() + ")");
        }
        applyFields(r, dto, actor);
        if (r.getStatus() == RequisitionStatus.REJECTED) {
            // Editing a rejected requisition reopens it as a draft; the old approval trail is cleared on resubmit.
            r.setStatus(RequisitionStatus.DRAFT);
            r.setStatusRemarks(null);
            r.getApprovals().clear();
        }
        requisitionRepository.save(r);
        auditService.log("UPDATE", "JobRequisition", r.getId(), "Updated requisition " + r.getRequisitionCode());
        return toDTO(r, actor);
    }

    // ============================================================ approval workflow

    @Override
    public RequisitionDTO submit(Long id, HiringActor actor) {
        JobRequisition r = getOrThrow(id);
        if (!(actor.staff() || r.getRequestedBy().getId().equals(actor.id()))) {
            throw new AccessDeniedException("Only the person who raised this requisition, a recruiter or an admin can submit it");
        }
        requireStatus(r, RequisitionStatus.DRAFT);

        r.getApprovals().clear();
        List<WorkflowStage> stages = workflowStageRepository
                .findByPrivilegeModule_CodeAndActiveTrueOrderBySequenceOrderAsc(AppConstants.MODULE_CODE_REQUISITION);

        for (WorkflowStage stage : stages) {
            Employee approver = approverResolver.resolve(stage, r.getRequestedBy());
            String skipReason = null;
            if (approver == null) {
                skipReason = "No eligible approver found - stage skipped";
            } else if (approver.getId().equals(r.getRequestedBy().getId())) {
                skipReason = "Requester cannot approve their own requisition - stage skipped";
                approver = null;
            }
            r.getApprovals().add(RequisitionApproval.builder()
                    .requisition(r)
                    .stage(stage)
                    .stageName(stage.getName())
                    .sequenceOrder(stage.getSequenceOrder())
                    .approverEmployee(approver)
                    .status(approver == null ? ApprovalStatus.SKIPPED : ApprovalStatus.PENDING)
                    .remarks(skipReason)
                    .actedAt(approver == null ? LocalDateTime.now() : null)
                    .build());
        }

        Optional<RequisitionApproval> first = nextPending(r);
        if (first.isEmpty()) {
            // No chain configured, or every stage resolved to nobody: don't leave it stuck.
            r.setStatus(RequisitionStatus.APPROVED);
            auditService.log("AUTO_APPROVE", "JobRequisition", r.getId(),
                    "Requisition " + r.getRequisitionCode() + " auto-approved (no approver stage applied)");
        } else {
            r.setStatus(RequisitionStatus.PENDING_APPROVAL);
            notifyApprover(first.get(), r);
            auditService.log("SUBMIT", "JobRequisition", r.getId(), "Submitted requisition " + r.getRequisitionCode() + " for approval");
        }
        requisitionRepository.save(r);
        return toDTO(r, actor);
    }

    @Override
    public RequisitionDTO approve(Long id, String remarks, HiringActor actor) {
        JobRequisition r = getOrThrow(id);
        requireStatus(r, RequisitionStatus.PENDING_APPROVAL);
        RequisitionApproval current = currentStageFor(r, actor);

        current.setStatus(ApprovalStatus.APPROVED);
        current.setRemarks(blankToNull(remarks));
        current.setActedAt(LocalDateTime.now());

        Optional<RequisitionApproval> next = nextPending(r);
        if (next.isPresent()) {
            notifyApprover(next.get(), r);
        } else {
            r.setStatus(RequisitionStatus.APPROVED);
            notifier.decided(r.getRequestedBy(), "Job requisition", r.getRequisitionCode(), "fully approved", null);
        }
        requisitionRepository.save(r);
        auditService.log("APPROVE", "JobRequisition", r.getId(),
                "Stage '" + current.getStageName() + "' approved by " + actor.employee().getFullName());
        return toDTO(r, actor);
    }

    @Override
    public RequisitionDTO reject(Long id, String remarks, HiringActor actor) {
        if (remarks == null || remarks.isBlank()) {
            throw new BusinessException("Remarks are required when rejecting a requisition");
        }
        JobRequisition r = getOrThrow(id);
        requireStatus(r, RequisitionStatus.PENDING_APPROVAL);
        RequisitionApproval current = currentStageFor(r, actor);

        current.setStatus(ApprovalStatus.REJECTED);
        current.setRemarks(remarks.trim());
        current.setActedAt(LocalDateTime.now());
        // Stages that were never reached should not look pending.
        r.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .forEach(a -> {
                    a.setStatus(ApprovalStatus.SKIPPED);
                    a.setRemarks("Not reached - requisition rejected at an earlier stage");
                    a.setActedAt(LocalDateTime.now());
                });
        r.setStatus(RequisitionStatus.REJECTED);
        r.setStatusRemarks(remarks.trim());
        requisitionRepository.save(r);

        notifier.decided(r.getRequestedBy(), "Job requisition", r.getRequisitionCode(), "rejected", remarks);
        auditService.log("REJECT", "JobRequisition", r.getId(),
                "Rejected at stage '" + current.getStageName() + "' by " + actor.employee().getFullName() + ": " + remarks);
        return toDTO(r, actor);
    }

    // ============================================================ status changes

    @Override
    public RequisitionDTO changeStatus(Long id, RequisitionAction action, String remarks, HiringActor actor) {
        JobRequisition r = getOrThrow(id);
        RequisitionStatus from = r.getStatus();
        boolean owner = access.isRequisitionOwner(actor, r);

        switch (action) {
            case OPEN -> {
                access.requireStaff(actor);
                requireStatus(r, RequisitionStatus.APPROVED);
                // Whoever opens it takes ownership as recruiter if none was assigned yet.
                if (r.getRecruiter() == null) {
                    r.setRecruiter(actor.employee());
                }
                r.setStatus(RequisitionStatus.OPEN);
            }
            case HOLD -> {
                requireStaffOrOwner(actor, owner);
                requireStatus(r, RequisitionStatus.OPEN);
                r.setStatus(RequisitionStatus.ON_HOLD);
            }
            case RESUME -> {
                requireStaffOrOwner(actor, owner);
                requireStatus(r, RequisitionStatus.ON_HOLD);
                r.setStatus(RequisitionStatus.OPEN);
            }
            case CLOSE -> {
                requireStaffOrOwner(actor, owner);
                if (from != RequisitionStatus.OPEN && from != RequisitionStatus.ON_HOLD) {
                    throw new BusinessException("Only an OPEN or ON_HOLD requisition can be closed (current status: " + from + ")");
                }
                r.setStatus(RequisitionStatus.CLOSED);
            }
            case CANCEL -> {
                if (!(actor.staff() || r.getRequestedBy().getId().equals(actor.id()))) {
                    throw new AccessDeniedException("Only the person who raised this requisition, a recruiter or an admin can cancel it");
                }
                if (from != RequisitionStatus.DRAFT && from != RequisitionStatus.PENDING_APPROVAL && from != RequisitionStatus.APPROVED) {
                    throw new BusinessException("Only DRAFT, PENDING_APPROVAL or APPROVED requisitions can be cancelled (current status: " + from + ")");
                }
                r.setStatus(RequisitionStatus.CANCELLED);
            }
        }
        r.setStatusRemarks(blankToNull(remarks));
        requisitionRepository.save(r);
        auditService.log(action.name(), "JobRequisition", r.getId(),
                "Requisition " + r.getRequisitionCode() + ": " + from + " -> " + r.getStatus()
                        + (remarks != null && !remarks.isBlank() ? " (" + remarks.trim() + ")" : ""));
        return toDTO(r, actor);
    }

    @Override
    public RequisitionDTO assignRecruiter(Long id, Long recruiterEmployeeId, HiringActor actor) {
        access.requireStaff(actor);
        JobRequisition r = getOrThrow(id);
        if (r.getStatus() == RequisitionStatus.CLOSED || r.getStatus() == RequisitionStatus.CANCELLED) {
            throw new BusinessException("A " + r.getStatus() + " requisition can no longer be reassigned");
        }
        Employee recruiter = employeeRepository.findById(recruiterEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + recruiterEmployeeId));
        boolean eligible = access.isRecruiter(recruiter.getId()) || isAdmin(recruiter);
        if (!eligible) {
            throw new BusinessException(recruiter.getFullName() + " is not a recruiter. Assign the 'Recruiter' role under Employee Access first.");
        }
        r.setRecruiter(recruiter);
        requisitionRepository.save(r);
        auditService.log("ASSIGN_RECRUITER", "JobRequisition", r.getId(),
                "Recruiter for " + r.getRequisitionCode() + " set to " + recruiter.getFullName());
        return toDTO(r, actor);
    }

    // ============================================================ reads

    @Override
    @Transactional(readOnly = true)
    public RequisitionDTO getById(Long id, HiringActor actor) {
        JobRequisition r = getOrThrow(id);
        access.requireCanViewRequisition(actor, r);
        return toDTO(r, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequisitionDTO> search(String keyword, RequisitionStatus status, Pageable pageable, HiringActor actor) {
        String kw = keyword == null ? "" : keyword.trim();
        Page<JobRequisition> page = actor.staff()
                ? requisitionRepository.searchAll(status, kw, pageable)
                : requisitionRepository.searchVisibleTo(actor.id(), status, kw, pageable);
        return Pages.map(page, r -> toDTO(r, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequisitionDTO> pendingApprovals(HiringActor actor) {
        return requisitionRepository.findPendingApprovalFor(actor.id()).stream()
                .map(r -> toDTO(r, actor))
                .toList();
    }

    // ============================================================ helpers

    private JobRequisition getOrThrow(Long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
    }

    private RequisitionDTO toDTO(JobRequisition r, HiringActor actor) {
        return mapper.toDTO(r, applicationRepository.countByRequisitionId(r.getId()), actor);
    }

    private void requireStatus(JobRequisition r, RequisitionStatus expected) {
        if (r.getStatus() != expected) {
            throw new BusinessException("This action requires status " + expected + " (current status: " + r.getStatus() + ")");
        }
    }

    private void requireStaffOrOwner(HiringActor actor, boolean owner) {
        if (!(actor.staff() || owner)) {
            throw new AccessDeniedException("Only recruiters, admins or the hiring manager can do this");
        }
    }

    private Optional<RequisitionApproval> nextPending(JobRequisition r) {
        return r.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .min(Comparator.comparing(RequisitionApproval::getSequenceOrder));
    }

    // The stage this person may act on RIGHT NOW: the lowest still-PENDING sequence.
    private RequisitionApproval currentStageFor(JobRequisition r, HiringActor actor) {
        RequisitionApproval current = nextPending(r)
                .orElseThrow(() -> new BusinessException("There is no pending approval stage on this requisition right now"));
        if (current.getApproverEmployee() == null || !current.getApproverEmployee().getId().equals(actor.id())) {
            throw new BusinessException("You are not the approver for the current stage (" + current.getStageName() + ") of this requisition");
        }
        return current;
    }

    private void notifyApprover(RequisitionApproval stage, JobRequisition r) {
        notifier.approvalRequested(stage.getApproverEmployee(), "Job requisition", r.getRequisitionCode(),
                "Title: " + r.getTitle() + "\nOpenings: " + r.getOpenings()
                        + "\nDepartment: " + r.getDepartment().getName()
                        + "\nRequested by: " + r.getRequestedBy().getFullName()
                        + "\nStage: " + stage.getStageName());
    }

    private void applyFields(JobRequisition r, RequisitionDTO dto, HiringActor actor) {
        if (dto.getBudgetMin() != null && dto.getBudgetMax() != null && dto.getBudgetMax().compareTo(dto.getBudgetMin()) < 0) {
            throw new BusinessException("Maximum budget cannot be lower than the minimum budget");
        }
        if (dto.getMinExperience() != null && dto.getMaxExperience() != null && dto.getMaxExperience().compareTo(dto.getMinExperience()) < 0) {
            throw new BusinessException("Maximum experience cannot be lower than the minimum experience");
        }
        if (r.getFilledCount() != null && dto.getOpenings() < r.getFilledCount()) {
            throw new BusinessException("Openings cannot be lower than the " + r.getFilledCount() + " position(s) already filled");
        }
        LocalDate target = dto.getTargetCloseDate();
        if (target != null && target.isBefore(LocalDate.now())
                && (r.getTargetCloseDate() == null || !target.equals(r.getTargetCloseDate()))) {
            throw new BusinessException("Target close date cannot be in the past");
        }

        r.setTitle(dto.getTitle().trim());
        r.setDepartment(departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId())));
        r.setDesignation(designationRepository.findById(dto.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + dto.getDesignationId())));
        r.setEmployeeType(employeeTypeRepository.findById(dto.getEmployeeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee type not found: " + dto.getEmployeeTypeId())));
        r.setLocation(dto.getLocationId() == null ? null : locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + dto.getLocationId())));
        r.setOpenings(dto.getOpenings());
        r.setBudgetMin(dto.getBudgetMin());
        r.setBudgetMax(dto.getBudgetMax());
        r.setMinExperience(dto.getMinExperience());
        r.setMaxExperience(dto.getMaxExperience());
        r.setJobDescription(blankToNull(dto.getJobDescription()));
        r.setJustification(blankToNull(dto.getJustification()));
        r.setTargetCloseDate(target);

        Long managerId = dto.getHiringManagerId() != null ? dto.getHiringManagerId() : actor.id();
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring manager not found: " + managerId));
        if (!Boolean.TRUE.equals(manager.getActive())) {
            throw new BusinessException("The hiring manager must be an active employee");
        }
        r.setHiringManager(manager);
    }

    private boolean isAdmin(Employee e) {
        return e.getUser() != null && e.getUser().getRoles().stream()
                .anyMatch(role -> AppConstants.ROLE_ADMIN.equals(role.getName()));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
