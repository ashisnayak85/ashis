package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.*;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.HiringRequests.ConvertRequest;
import com.enterprise.ems.dto.OfferDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.HiringMapper;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.EmployeeService;
import com.enterprise.ems.service.OfferService;
import com.enterprise.ems.service.hiring.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/*
 * PURPOSE: Offer lifecycle - create (only after a completed interview), approve through the
 * admin-configured OFFER chain, send, accept / decline / expire / revoke, and finally convert the
 * accepted offer into an Employee via the EXISTING EmployeeService.create() (so every employee
 * rule - duplicate code/email/Aadhaar, required fields - stays enforced in exactly one place).
 *
 * APPLICATION STAGE LINKAGE (all through ApplicationStageHelper):
 *   offer created  -> application INTERVIEW -> OFFER
 *   offer revoked / expired -> application OFFER -> INTERVIEW  (a revised offer can be made)
 *   offer declined -> application OFFER -> WITHDRAWN
 *   offer converted -> application OFFER -> HIRED
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OfferServiceImpl implements OfferService {

    private static final Logger log = LoggerFactory.getLogger(OfferServiceImpl.class);
    private static final DateTimeFormatter LETTER_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final OfferRepository offerRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobRequisitionRepository requisitionRepository;
    private final InterviewRepository interviewRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final WorkflowApproverResolver approverResolver;
    private final EmployeeService employeeService;
    private final EmployeeCodeGenerator employeeCodeGenerator;
    private final ApplicationStageHelper stageHelper;
    private final HiringAccessService access;
    private final HiringMapper mapper;
    private final HiringNotifier notifier;
    private final AuditService auditService;

    @Value("${app.hiring.company-name:Our Company}")
    private String companyName;

    // ============================================================ create / edit

    @Override
    public OfferDTO create(Long applicationId, OfferDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        if (application.getStage() != ApplicationStage.INTERVIEW) {
            throw new BusinessException("An offer can only be made for an application in the INTERVIEW stage (current: " + application.getStage() + ")");
        }
        if (application.getRequisition().getStatus() != RequisitionStatus.OPEN) {
            throw new BusinessException("The requisition is " + application.getRequisition().getStatus() + " - offers can only be made while it is OPEN");
        }
        if (!interviewRepository.existsByApplicationIdAndStatus(applicationId, InterviewStatus.COMPLETED)) {
            throw new BusinessException("Complete at least one interview round (with feedback) before making an offer");
        }
        if (offerRepository.existsByApplicationIdAndStatusIn(applicationId, OfferStatus.OPEN_STATUSES)) {
            throw new BusinessException("This application already has an open offer - revoke it before creating another");
        }

        Offer offer = Offer.builder()
                .application(application)
                .createdBy(actor.employee())
                .status(OfferStatus.DRAFT)
                .build();
        applyFields(offer, dto, application);
        offer = offerRepository.save(offer);

        stageHelper.transition(application, ApplicationStage.OFFER, actor.employee(), "Offer created");
        auditService.log("CREATE", "Offer", offer.getId(), "Offer drafted for " + application.getCandidate().getFullName());
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO update(Long id, OfferDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.DRAFT);
        applyFields(offer, dto, offer.getApplication());
        offerRepository.save(offer);
        auditService.log("UPDATE", "Offer", offer.getId(), "Offer updated");
        return mapper.toDTO(offer, actor);
    }

    // ============================================================ approval workflow

    @Override
    public OfferDTO submit(Long id, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.DRAFT);

        JobRequisition requisition = offer.getApplication().getRequisition();
        offer.getApprovals().clear();
        List<WorkflowStage> stages = workflowStageRepository
                .findByPrivilegeModule_CodeAndActiveTrueOrderBySequenceOrderAsc(AppConstants.MODULE_CODE_OFFER);

        for (WorkflowStage stage : stages) {
            // Offer chains are relative to the hiring manager (e.g. REPORTING_MANAGER = the hiring manager's manager).
            Employee approver = approverResolver.resolve(stage, requisition.getHiringManager());
            String skipReason = null;
            if (approver == null) {
                skipReason = "No eligible approver found - stage skipped";
            } else if (offer.getCreatedBy() != null && approver.getId().equals(offer.getCreatedBy().getId())) {
                skipReason = "The person who drafted the offer cannot approve it - stage skipped";
                approver = null;
            }
            offer.getApprovals().add(OfferApproval.builder()
                    .offer(offer)
                    .stage(stage)
                    .stageName(stage.getName())
                    .sequenceOrder(stage.getSequenceOrder())
                    .approverEmployee(approver)
                    .status(approver == null ? ApprovalStatus.SKIPPED : ApprovalStatus.PENDING)
                    .remarks(skipReason)
                    .actedAt(approver == null ? LocalDateTime.now() : null)
                    .build());
        }

        Optional<OfferApproval> first = nextPending(offer);
        if (first.isEmpty()) {
            offer.setStatus(OfferStatus.APPROVED);
            auditService.log("AUTO_APPROVE", "Offer", offer.getId(), "Offer auto-approved (no approver stage applied)");
        } else {
            offer.setStatus(OfferStatus.PENDING_APPROVAL);
            notifyApprover(first.get(), offer);
            auditService.log("SUBMIT", "Offer", offer.getId(), "Offer submitted for approval");
        }
        offerRepository.save(offer);
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO approve(Long id, String remarks, HiringActor actor) {
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.PENDING_APPROVAL);
        OfferApproval current = currentStageFor(offer, actor);

        current.setStatus(ApprovalStatus.APPROVED);
        current.setRemarks(blankToNull(remarks));
        current.setActedAt(LocalDateTime.now());

        Optional<OfferApproval> next = nextPending(offer);
        if (next.isPresent()) {
            notifyApprover(next.get(), offer);
        } else {
            offer.setStatus(OfferStatus.APPROVED);
            notifier.decided(offer.getCreatedBy(), "Job offer", offerRef(offer), "fully approved", null);
        }
        offerRepository.save(offer);
        auditService.log("APPROVE", "Offer", offer.getId(),
                "Stage '" + current.getStageName() + "' approved by " + actor.employee().getFullName());
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO reject(Long id, String remarks, HiringActor actor) {
        if (remarks == null || remarks.isBlank()) {
            throw new BusinessException("Remarks are required when rejecting an offer");
        }
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.PENDING_APPROVAL);
        OfferApproval current = currentStageFor(offer, actor);

        current.setStatus(ApprovalStatus.REJECTED);
        current.setRemarks(remarks.trim());
        current.setActedAt(LocalDateTime.now());
        offer.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .forEach(a -> {
                    a.setStatus(ApprovalStatus.SKIPPED);
                    a.setRemarks("Not reached - offer rejected at an earlier stage");
                    a.setActedAt(LocalDateTime.now());
                });
        // Back to DRAFT so the recruiter can revise and resubmit; the rejection stays visible in the approval trail.
        offer.setStatus(OfferStatus.DRAFT);
        offerRepository.save(offer);

        notifier.decided(offer.getCreatedBy(), "Job offer", offerRef(offer), "rejected (returned to draft)", remarks);
        auditService.log("REJECT", "Offer", offer.getId(),
                "Rejected at stage '" + current.getStageName() + "' by " + actor.employee().getFullName() + ": " + remarks);
        return mapper.toDTO(offer, actor);
    }

    // ============================================================ candidate-facing steps

    @Override
    public OfferDTO send(Long id, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.APPROVED);
        if (offer.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException("The offer expiry date has already passed - edit the dates (revoke and create a new offer) before sending");
        }
        offer.setStatus(OfferStatus.SENT);
        offer.setSentAt(LocalDateTime.now());
        offerRepository.save(offer);

        notifier.offerSent(offer.getApplication().getCandidate(), offer);
        auditService.log("SEND", "Offer", offer.getId(), "Offer sent to " + offer.getApplication().getCandidate().getFullName());
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO accept(Long id, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.SENT);
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
        auditService.log("ACCEPT", "Offer", offer.getId(), "Offer accepted by " + offer.getApplication().getCandidate().getFullName());
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO decline(Long id, String reason, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        requireStatus(offer, OfferStatus.SENT);
        offer.setStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(LocalDateTime.now());
        offer.setClosingReason(blankToNull(reason));
        offerRepository.save(offer);

        JobApplication application = offer.getApplication();
        if (application.getStage() == ApplicationStage.OFFER) {
            stageHelper.transition(application, ApplicationStage.WITHDRAWN, actor.employee(),
                    "Offer declined" + (blankToNull(reason) != null ? ": " + reason.trim() : ""));
        }
        auditService.log("DECLINE", "Offer", offer.getId(), "Offer declined by " + application.getCandidate().getFullName());
        return mapper.toDTO(offer, actor);
    }

    @Override
    public OfferDTO revoke(Long id, String reason, HiringActor actor) {
        access.requireStaff(actor);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required to revoke an offer");
        }
        Offer offer = getOrThrow(id);
        if (!offer.getStatus().isOpen()) {
            throw new BusinessException("Only an open offer can be revoked (current status: " + offer.getStatus() + ")");
        }
        offer.setStatus(OfferStatus.REVOKED);
        offer.setClosingReason(reason.trim());
        offerRepository.save(offer);

        JobApplication application = offer.getApplication();
        if (application.getStage() == ApplicationStage.OFFER) {
            // Back to INTERVIEW so a revised offer can be created.
            stageHelper.transition(application, ApplicationStage.INTERVIEW, actor.employee(), "Offer revoked: " + reason.trim());
        }
        auditService.log("REVOKE", "Offer", offer.getId(), "Offer revoked: " + reason.trim());
        return mapper.toDTO(offer, actor);
    }

    // ============================================================ convert to employee

    @Override
    public EmployeeDTO convertToEmployee(Long id, ConvertRequest req, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new BusinessException("Only an ACCEPTED offer can be converted to an employee (current status: " + offer.getStatus() + ")");
        }
        JobApplication application = offer.getApplication();
        if (application.getStage() != ApplicationStage.OFFER) {
            throw new BusinessException("The application is in stage " + application.getStage() + " - it cannot be converted");
        }
        Candidate candidate = application.getCandidate();
        JobRequisition requisition = application.getRequisition();

        // Rehire: Employee.email is unique, so a returning person collides here. Rehire is NOT automated yet -
        // fail early with a clear message instead of a raw duplicate error from deep inside employee creation.
        if (employeeRepository.existsByEmail(candidate.getEmail())) {
            throw new BusinessException("An employee record with email " + candidate.getEmail()
                    + " already exists (possible rehire). Reactivate that employee instead, or change the candidate's email, then retry.");
        }

        String firstName = blankToNull(req == null ? null : req.firstName()) != null ? req.firstName().trim() : candidate.getFirstName();
        String lastName = blankToNull(req == null ? null : req.lastName()) != null ? req.lastName().trim() : candidate.getLastName();
        if (firstName == null || firstName.trim().length() < 2 || lastName == null || lastName.trim().length() < 2) {
            throw new BusinessException("An employee needs a first and last name of at least 2 characters each - enter them in the convert form");
        }

        LocalDate joining = req != null && req.dateOfJoining() != null ? req.dateOfJoining() : offer.getJoiningDate();
        // Annual CTC -> monthly by default; the recruiter can override to match how payroll stores Employee.salary.
        BigDecimal salary = req != null && req.salary() != null
                ? req.salary()
                : offer.getAnnualCtc().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        Long managerId = req != null && req.reportingManagerId() != null ? req.reportingManagerId() : requisition.getHiringManager().getId();
        Long locationId = req != null && req.locationId() != null ? req.locationId()
                : (requisition.getLocation() != null ? requisition.getLocation().getId() : null);
        String code = req != null && blankToNull(req.employeeCode()) != null ? req.employeeCode().trim() : employeeCodeGenerator.next();

        EmployeeDTO employeeDTO = EmployeeDTO.builder()
                .employeeCode(code)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(candidate.getEmail())
                .mobile(candidate.getMobile())
                .dateOfBirth(req == null ? null : req.dateOfBirth())
                .dateOfJoining(joining)
                .salary(salary)
                .gender(req == null ? null : blankToNull(req.gender()))
                .totalExperience(candidate.getTotalExperience())
                .designationId(offer.getDesignation().getId())
                .departmentId(requisition.getDepartment().getId())
                .employeeTypeId(requisition.getEmployeeType().getId())
                .locationId(locationId)
                .reportingManagerId(managerId)
                .active(true)
                .build();

        // Single source of truth for employee creation (duplicate checks, mapping, audit).
        EmployeeDTO created = employeeService.create(employeeDTO);
        Employee employee = employeeRepository.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found after creation: " + created.getId()));

        offer.setConvertedEmployee(employee);
        offer.setStatus(OfferStatus.CONVERTED);
        offerRepository.save(offer);

        application.setHiredEmployee(employee);
        stageHelper.transition(application, ApplicationStage.HIRED, actor.employee(), "Converted to employee " + employee.getEmployeeCode());

        requisition.setFilledCount(requisition.getFilledCount() + 1);
        if (requisition.getFilledCount() >= requisition.getOpenings()) {
            requisition.setStatus(RequisitionStatus.CLOSED);
            requisition.setStatusRemarks("All positions filled");
        }
        requisitionRepository.save(requisition);

        auditService.log("CONVERT", "Offer", offer.getId(),
                candidate.getFullName() + " converted to employee " + employee.getEmployeeCode());
        return created;
    }

    // ============================================================ reads

    @Override
    public OfferDTO getById(Long id, HiringActor actor) {
        Offer offer = getOrThrow(id);
        requireCanView(offer, actor);
        return mapper.toDTO(offer, actor);
    }

    @Override
    public List<OfferDTO> listByApplication(Long applicationId, HiringActor actor) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        boolean hiringManager = application.getRequisition().getHiringManager().getId().equals(actor.id());
        if (!(actor.staff() || hiringManager)) {
            throw new AccessDeniedException("You do not have access to this application's offers");
        }
        return offerRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .peek(this::expireIfNeeded)
                .map(o -> mapper.toDTO(o, actor))
                .toList();
    }

    @Override
    public PageResponse<OfferDTO> search(String keyword, OfferStatus status, Pageable pageable, HiringActor actor) {
        access.requireStaff(actor);
        String kw = keyword == null ? "" : keyword.trim();
        return Pages.map(offerRepository.search(status, kw, pageable), o -> {
            expireIfNeeded(o);
            return mapper.toDTO(o, actor);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferDTO> pendingApprovals(HiringActor actor) {
        return offerRepository.findPendingApprovalFor(actor.id()).stream()
                .map(o -> mapper.toDTO(o, actor))
                .toList();
    }

    @Override
    public String renderLetterHtml(Long id, HiringActor actor) {
        access.requireStaff(actor);
        Offer offer = getOrThrow(id);
        if (offer.getStatus() == OfferStatus.DRAFT || offer.getStatus() == OfferStatus.PENDING_APPROVAL) {
            throw new BusinessException("The offer letter is available once the offer has been approved");
        }
        Candidate c = offer.getApplication().getCandidate();
        JobRequisition r = offer.getApplication().getRequisition();

        String name = HtmlUtils.htmlEscape(c.getFullName());
        String company = HtmlUtils.htmlEscape(companyName);
        String designation = HtmlUtils.htmlEscape(offer.getDesignation().getName());
        String department = HtmlUtils.htmlEscape(r.getDepartment().getName());
        String location = r.getLocation() != null ? HtmlUtils.htmlEscape(r.getLocation().getName()) : "as communicated";
        String ctc = String.format("%,.2f", offer.getAnnualCtc());

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Offer letter - " + name + "</title>"
                + "<style>body{font-family:Arial,Helvetica,sans-serif;max-width:780px;margin:40px auto;padding:0 24px;color:#222;line-height:1.55}"
                + "h1{font-size:22px;margin-bottom:4px}.muted{color:#666}table{border-collapse:collapse;margin:16px 0;width:100%}"
                + "td{border:1px solid #ccc;padding:8px 12px}td:first-child{background:#f5f5f5;width:35%;font-weight:bold}"
                + ".sign{margin-top:56px}button{margin:16px 0;padding:8px 16px}@media print{button{display:none}}</style></head><body>"
                + "<button onclick=\"window.print()\">Print / Save as PDF</button>"
                + "<h1>" + company + "</h1><div class=\"muted\">Offer of employment</div><hr>"
                + "<p>Date: " + LocalDate.now().format(LETTER_DATE) + "</p>"
                + "<p>Dear " + name + ",</p>"
                + "<p>We are pleased to offer you the position of <strong>" + designation + "</strong> in the "
                + department + " department at " + company + ". The terms of this offer are as follows:</p>"
                + "<table>"
                + "<tr><td>Position</td><td>" + designation + "</td></tr>"
                + "<tr><td>Department</td><td>" + department + "</td></tr>"
                + "<tr><td>Work location</td><td>" + location + "</td></tr>"
                + "<tr><td>Annual compensation (CTC)</td><td>" + ctc + "</td></tr>"
                + "<tr><td>Proposed joining date</td><td>" + offer.getJoiningDate().format(LETTER_DATE) + "</td></tr>"
                + "<tr><td>This offer is valid until</td><td>" + offer.getExpiryDate().format(LETTER_DATE) + "</td></tr>"
                + "</table>"
                + (offer.getRemarks() != null ? "<p>" + HtmlUtils.htmlEscape(offer.getRemarks()) + "</p>" : "")
                + "<p>This offer is subject to satisfactory completion of our standard pre-employment verification. "
                + "Please confirm your acceptance before the validity date mentioned above.</p>"
                + "<p>We look forward to welcoming you to the team.</p>"
                + "<div class=\"sign\"><p>Sincerely,</p><p><strong>" + company + " &ndash; Human Resources</strong></p></div>"
                + "</body></html>";
    }

    // ============================================================ nightly expiry

    // Sent offers whose expiry date has passed become EXPIRED, and the candidate goes back to INTERVIEW
    // so a recruiter can decide what to do (revised offer / reject).
    @Scheduled(cron = "0 15 0 * * *")
    public void expireOverdueOffers() {
        List<Offer> overdue = offerRepository.findByStatusAndExpiryDateBefore(OfferStatus.SENT, LocalDate.now());
        overdue.forEach(this::expireIfNeeded);
        if (!overdue.isEmpty()) {
            log.info("Expired {} overdue job offer(s)", overdue.size());
        }
    }

    // ============================================================ helpers

    private Offer getOrThrow(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + id));
        expireIfNeeded(offer);
        return offer;
    }

    // Lazy expiry as well, so a screen opened before the nightly job still shows the truth.
    private void expireIfNeeded(Offer offer) {
        if (offer.getStatus() == OfferStatus.SENT && offer.getExpiryDate().isBefore(LocalDate.now())) {
            offer.setStatus(OfferStatus.EXPIRED);
            offer.setClosingReason("Offer expired on " + offer.getExpiryDate());
            offerRepository.save(offer);
            JobApplication application = offer.getApplication();
            if (application.getStage() == ApplicationStage.OFFER) {
                stageHelper.transition(application, ApplicationStage.INTERVIEW, null, "Offer expired");
            }
        }
    }

    private void requireStatus(Offer offer, OfferStatus expected) {
        if (offer.getStatus() != expected) {
            throw new BusinessException("This action requires status " + expected + " (current status: " + offer.getStatus() + ")");
        }
    }

    private void requireCanView(Offer offer, HiringActor actor) {
        boolean hiringManager = offer.getApplication().getRequisition().getHiringManager().getId().equals(actor.id());
        boolean approver = offer.getApprovals().stream()
                .anyMatch(a -> a.getApproverEmployee() != null && a.getApproverEmployee().getId().equals(actor.id()));
        if (!(actor.staff() || hiringManager || approver)) {
            throw new AccessDeniedException("You do not have access to this offer");
        }
    }

    private Optional<OfferApproval> nextPending(Offer offer) {
        return offer.getApprovals().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .min(Comparator.comparing(OfferApproval::getSequenceOrder));
    }

    private OfferApproval currentStageFor(Offer offer, HiringActor actor) {
        OfferApproval current = nextPending(offer)
                .orElseThrow(() -> new BusinessException("There is no pending approval stage on this offer right now"));
        if (current.getApproverEmployee() == null || !current.getApproverEmployee().getId().equals(actor.id())) {
            throw new BusinessException("You are not the approver for the current stage (" + current.getStageName() + ") of this offer");
        }
        return current;
    }

    private void notifyApprover(OfferApproval stage, Offer offer) {
        Candidate c = offer.getApplication().getCandidate();
        notifier.approvalRequested(stage.getApproverEmployee(), "Job offer", offerRef(offer),
                "Candidate: " + c.getFullName()
                        + "\nPosition: " + offer.getDesignation().getName()
                        + "\nAnnual CTC: " + String.format("%,.2f", offer.getAnnualCtc())
                        + "\nStage: " + stage.getStageName());
    }

    private String offerRef(Offer offer) {
        return "#" + offer.getId() + " (" + offer.getApplication().getCandidate().getFullName() + ")";
    }

    private void applyFields(Offer offer, OfferDTO dto, JobApplication application) {
        LocalDate today = LocalDate.now();
        if (dto.getExpiryDate().isBefore(today)) {
            throw new BusinessException("The offer expiry date cannot be in the past");
        }
        if (dto.getJoiningDate().isBefore(today)) {
            throw new BusinessException("The joining date cannot be in the past");
        }
        BigDecimal budgetMax = application.getRequisition().getBudgetMax();
        boolean aboveBudget = budgetMax != null && dto.getAnnualCtc().compareTo(budgetMax) > 0;
        if (aboveBudget && blankToNull(dto.getBudgetExceptionReason()) == null) {
            throw new BusinessException("This offer is above the requisition's maximum budget (" + String.format("%,.2f", budgetMax)
                    + ") - a budget exception reason is required");
        }

        offer.setDesignation(designationRepository.findById(dto.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + dto.getDesignationId())));
        offer.setAnnualCtc(dto.getAnnualCtc());
        offer.setJoiningDate(dto.getJoiningDate());
        offer.setExpiryDate(dto.getExpiryDate());
        offer.setBudgetExceptionReason(aboveBudget ? dto.getBudgetExceptionReason().trim() : null);
        offer.setRemarks(blankToNull(dto.getRemarks()));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
