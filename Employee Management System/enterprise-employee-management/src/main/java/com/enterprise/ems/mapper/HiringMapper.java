package com.enterprise.ems.mapper;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.constant.ApprovalStatus;
import com.enterprise.ems.constant.InterviewStatus;
import com.enterprise.ems.constant.OfferStatus;
import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.hiring.HiringActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/*
 * PURPOSE: Entity -> DTO conversion for the whole hiring module.
 *
 * Most "who can see what" decisions that shape the OUTPUT (hiding salary
 * expectations, blind interview feedback, per-viewer button hints) are made here,
 * because this is the last step before data leaves the service layer.
 */
@Component
@RequiredArgsConstructor
public class HiringMapper {

    private final EmployeeRepository employeeRepository;

    // ------------------------------------------------------------------ approvals

    public ApprovalDTO toDTO(RequisitionApproval a) {
        return ApprovalDTO.builder()
                .id(a.getId())
                .stageName(a.getStageName())
                .sequenceOrder(a.getSequenceOrder())
                .approverId(a.getApproverEmployee() != null ? a.getApproverEmployee().getId() : null)
                .approverName(a.getApproverEmployee() != null ? a.getApproverEmployee().getFullName() : null)
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .actedAt(a.getActedAt())
                .build();
    }

    public ApprovalDTO toDTO(OfferApproval a) {
        return ApprovalDTO.builder()
                .id(a.getId())
                .stageName(a.getStageName())
                .sequenceOrder(a.getSequenceOrder())
                .approverId(a.getApproverEmployee() != null ? a.getApproverEmployee().getId() : null)
                .approverName(a.getApproverEmployee() != null ? a.getApproverEmployee().getFullName() : null)
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .actedAt(a.getActedAt())
                .build();
    }

    // ------------------------------------------------------------------ requisition

    public RequisitionDTO toDTO(JobRequisition r, long applicationCount, HiringActor viewer) {
        Optional<RequisitionApproval> current = r.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .min(Comparator.comparing(RequisitionApproval::getSequenceOrder));

        boolean viewerCanApprove = r.getStatus() == RequisitionStatus.PENDING_APPROVAL
                && current.isPresent()
                && current.get().getApproverEmployee() != null
                && current.get().getApproverEmployee().getId().equals(viewer.id());

        boolean owner = r.getRequestedBy().getId().equals(viewer.id()) || r.getHiringManager().getId().equals(viewer.id());
        boolean editableStatus = r.getStatus() == RequisitionStatus.DRAFT || r.getStatus() == RequisitionStatus.REJECTED;

        return RequisitionDTO.builder()
                .id(r.getId())
                .requisitionCode(r.getRequisitionCode())
                .title(r.getTitle())
                .departmentId(r.getDepartment().getId())
                .departmentName(r.getDepartment().getName())
                .designationId(r.getDesignation().getId())
                .designationName(r.getDesignation().getName())
                .employeeTypeId(r.getEmployeeType().getId())
                .employeeTypeName(r.getEmployeeType().getName())
                .locationId(r.getLocation() != null ? r.getLocation().getId() : null)
                .locationName(r.getLocation() != null ? r.getLocation().getName() : null)
                .openings(r.getOpenings())
                .filledCount(r.getFilledCount())
                .budgetMin(r.getBudgetMin())
                .budgetMax(r.getBudgetMax())
                .minExperience(r.getMinExperience())
                .maxExperience(r.getMaxExperience())
                .jobDescription(r.getJobDescription())
                .justification(r.getJustification())
                .hiringManagerId(r.getHiringManager().getId())
                .hiringManagerName(r.getHiringManager().getFullName())
                .recruiterId(r.getRecruiter() != null ? r.getRecruiter().getId() : null)
                .recruiterName(r.getRecruiter() != null ? r.getRecruiter().getFullName() : null)
                .requestedById(r.getRequestedBy().getId())
                .requestedByName(r.getRequestedBy().getFullName())
                .status(r.getStatus())
                .targetCloseDate(r.getTargetCloseDate())
                .statusRemarks(r.getStatusRemarks())
                .approvals(r.getApprovals().stream().map(this::toDTO).toList())
                .applicationCount(applicationCount)
                .viewerCanApprove(viewerCanApprove)
                .viewerCanEdit(editableStatus && (viewer.staff() || r.getRequestedBy().getId().equals(viewer.id())))
                .viewerCanManage(viewer.staff() || owner)
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ------------------------------------------------------------------ candidate

    /** includeSensitive = viewer is hiring staff: salary figures and the "already an employee" hint are only for them. */
    public CandidateDTO toDTO(Candidate c, boolean includeSensitive) {
        Long existingEmployeeId = null;
        if (includeSensitive && Boolean.TRUE.equals(c.getActive())) {
            existingEmployeeId = employeeRepository.findByEmail(c.getEmail()).map(Employee::getId).orElse(null);
        }
        return CandidateDTO.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .mobile(c.getMobile())
                .currentCompany(c.getCurrentCompany())
                .currentDesignation(c.getCurrentDesignation())
                .currentLocation(c.getCurrentLocation())
                .totalExperience(c.getTotalExperience())
                .currentCtc(includeSensitive ? c.getCurrentCtc() : null)
                .expectedCtc(includeSensitive ? c.getExpectedCtc() : null)
                .noticePeriodDays(c.getNoticePeriodDays())
                .skills(c.getSkills())
                .source(c.getSource())
                .vendorName(c.getVendorName())
                .referredById(c.getReferredBy() != null ? c.getReferredBy().getId() : null)
                .referredByName(c.getReferredBy() != null ? c.getReferredBy().getFullName() : null)
                .consentGivenAt(c.getConsentGivenAt())
                .consentGiven(c.getConsentGivenAt() != null)
                .resumeAvailable(c.getResumeStoredName() != null)
                .resumeFileName(c.getResumeOriginalName())
                .resumeUploadedAt(c.getResumeUploadedAt())
                .active(c.getActive())
                .existingEmployeeId(existingEmployeeId)
                .createdAt(c.getCreatedAt())
                .build();
    }

    // ------------------------------------------------------------------ application

    public ApplicationDTO toDTO(JobApplication a, boolean includeSensitive) {
        Candidate c = a.getCandidate();
        JobRequisition r = a.getRequisition();
        List<ApplicationStage> next = Arrays.stream(ApplicationStage.values())
                .filter(s -> a.getStage().canMoveTo(s) && s != ApplicationStage.OFFER && s != ApplicationStage.HIRED)
                .toList();
        return ApplicationDTO.builder()
                .id(a.getId())
                .candidateId(c.getId())
                .candidateName(c.getFullName())
                .candidateEmail(c.getEmail())
                .candidateMobile(c.getMobile())
                .candidateExperience(c.getTotalExperience())
                .candidateCurrentCompany(c.getCurrentCompany())
                .candidateNoticePeriodDays(c.getNoticePeriodDays())
                .candidateExpectedCtc(includeSensitive ? c.getExpectedCtc() : null)
                .candidateSource(c.getSource())
                .requisitionId(r.getId())
                .requisitionCode(r.getRequisitionCode())
                .requisitionTitle(r.getTitle())
                .stage(a.getStage())
                .rejectionReason(a.getRejectionReason())
                .appliedOn(a.getAppliedOn())
                .stageChangedAt(a.getStageChangedAt())
                .hiredEmployeeId(a.getHiredEmployee() != null ? a.getHiredEmployee().getId() : null)
                .allowedNextStages(next)
                .build();
    }

    public ApplicationHistoryDTO toDTO(ApplicationStageHistory h) {
        return ApplicationHistoryDTO.builder()
                .id(h.getId())
                .fromStage(h.getFromStage())
                .toStage(h.getToStage())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                .changedAt(h.getChangedAt())
                .remarks(h.getRemarks())
                .build();
    }

    // ------------------------------------------------------------------ interview

    /**
     * BLIND FEEDBACK: a panelist can read other panelists' ratings/comments only after submitting
     * their own (so nobody is anchored by a colleague's opinion). Hiring staff and the requisition's
     * hiring manager see every SUBMITTED feedback. Whether someone has submitted is always visible.
     */
    public InterviewDTO toDTO(Interview i, HiringActor viewer) {
        JobApplication app = i.getApplication();
        Candidate c = app.getCandidate();
        boolean seesAllSubmitted = viewer.staff() || app.getRequisition().getHiringManager().getId().equals(viewer.id());

        InterviewPanelist mine = i.getPanel().stream()
                .filter(p -> p.getInterviewer().getId().equals(viewer.id()))
                .findFirst().orElse(null);
        boolean viewerHasSubmitted = mine != null && mine.isSubmitted();

        List<PanelistDTO> panel = i.getPanel().stream().map(p -> {
            boolean isMine = mine != null && p.getId() != null && p.getId().equals(mine.getId());
            boolean canSeeContent = p.isSubmitted() && (seesAllSubmitted || isMine || viewerHasSubmitted);
            return PanelistDTO.builder()
                    .id(p.getId())
                    .interviewerId(p.getInterviewer().getId())
                    .interviewerName(p.getInterviewer().getFullName())
                    .submitted(p.isSubmitted())
                    .rating(canSeeContent ? p.getRating() : null)
                    .recommendation(canSeeContent ? p.getRecommendation() : null)
                    .comments(canSeeContent ? p.getComments() : null)
                    .submittedAt(p.getSubmittedAt())
                    .build();
        }).toList();

        return InterviewDTO.builder()
                .id(i.getId())
                .applicationId(app.getId())
                .candidateId(c.getId())
                .candidateName(c.getFullName())
                .requisitionId(app.getRequisition().getId())
                .requisitionTitle(app.getRequisition().getTitle())
                .roundNumber(i.getRoundNumber())
                .roundName(i.getRoundName())
                .mode(i.getMode())
                .scheduledAt(i.getScheduledAt())
                .durationMinutes(i.getDurationMinutes())
                .locationOrLink(i.getLocationOrLink())
                .status(i.getStatus())
                .cancelReason(i.getCancelReason())
                .panel(panel)
                .viewerIsPanelist(mine != null)
                .viewerHasSubmitted(viewerHasSubmitted)
                .build();
    }

    // ------------------------------------------------------------------ offer

    public OfferDTO toDTO(Offer o, HiringActor viewer) {
        JobApplication app = o.getApplication();
        JobRequisition r = app.getRequisition();

        Optional<OfferApproval> current = o.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .min(Comparator.comparing(OfferApproval::getSequenceOrder));
        boolean viewerCanApprove = o.getStatus() == OfferStatus.PENDING_APPROVAL
                && current.isPresent()
                && current.get().getApproverEmployee() != null
                && current.get().getApproverEmployee().getId().equals(viewer.id());

        boolean aboveBudget = r.getBudgetMax() != null && o.getAnnualCtc().compareTo(r.getBudgetMax()) > 0;

        return OfferDTO.builder()
                .id(o.getId())
                .applicationId(app.getId())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFullName())
                .requisitionId(r.getId())
                .requisitionCode(r.getRequisitionCode())
                .requisitionTitle(r.getTitle())
                .designationId(o.getDesignation().getId())
                .designationName(o.getDesignation().getName())
                .annualCtc(o.getAnnualCtc())
                .joiningDate(o.getJoiningDate())
                .expiryDate(o.getExpiryDate())
                .budgetExceptionReason(o.getBudgetExceptionReason())
                .remarks(o.getRemarks())
                .status(o.getStatus())
                .sentAt(o.getSentAt())
                .respondedAt(o.getRespondedAt())
                .closingReason(o.getClosingReason())
                .convertedEmployeeId(o.getConvertedEmployee() != null ? o.getConvertedEmployee().getId() : null)
                .requisitionBudgetMin(r.getBudgetMin())
                .requisitionBudgetMax(r.getBudgetMax())
                .aboveBudget(aboveBudget)
                .approvals(o.getApprovals().stream().map(this::toDTO).toList())
                .viewerCanApprove(viewerCanApprove)
                .createdAt(o.getCreatedAt())
                .build();
    }

    public boolean isActive(Interview i) {
        return i.getStatus() == InterviewStatus.SCHEDULED;
    }
}
