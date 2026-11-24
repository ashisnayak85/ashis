package com.enterprise.ems.service.hiring;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.JobApplication;
import com.enterprise.ems.entity.JobRequisition;
import com.enterprise.ems.repository.EmployeeWorkflowRoleRepository;
import com.enterprise.ems.repository.InterviewRepository;
import com.enterprise.ems.repository.JobApplicationRepository;
import com.enterprise.ems.security.CurrentEmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: The single place that decides "may this person see / do this" in the
 * hiring module. Controllers only authenticate; every ownership / involvement
 * rule lives here so it can't be forgotten in one endpoint.
 *
 * Denials throw Spring's AccessDeniedException, which GlobalExceptionHandler
 * turns into a 403 JSON response.
 */
@Component
@RequiredArgsConstructor
public class HiringAccessService {

    private final CurrentEmployeeResolver currentEmployeeResolver;
    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;
    private final JobApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public HiringActor actor(UserDetails principal) {
        Employee employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        boolean admin = hasAuthority(principal, AppConstants.ROLE_ADMIN);
        boolean manager = hasAuthority(principal, AppConstants.ROLE_MANAGER);
        boolean recruiter = isRecruiter(employee.getId());
        return new HiringActor(employee, admin, manager, recruiter);
    }

    public boolean isRecruiter(Long employeeId) {
        return employeeWorkflowRoleRepository
                .existsByEmployeeIdAndWorkflowRole_NameIgnoreCaseAndActiveTrue(employeeId, AppConstants.WORKFLOW_ROLE_RECRUITER);
    }

    // ---------------- hard requirements (throw) ----------------

    public void requireStaff(HiringActor actor) {
        if (!actor.staff()) {
            throw new AccessDeniedException("Only recruiters and admins can do this");
        }
    }

    public void requireAdmin(HiringActor actor) {
        if (!actor.admin()) {
            throw new AccessDeniedException("Only an admin can do this");
        }
    }

    public void requireCanRaiseRequisition(HiringActor actor) {
        if (!actor.canRaiseRequisition()) {
            throw new AccessDeniedException("Only managers, recruiters and admins can raise or look up hiring requisitions");
        }
    }

    // ---------------- soft checks (boolean) ----------------

    /** The hiring manager or the person who raised it. */
    public boolean isRequisitionOwner(HiringActor actor, JobRequisition r) {
        return r.getHiringManager().getId().equals(actor.id()) || r.getRequestedBy().getId().equals(actor.id());
    }

    public boolean isRequisitionApprover(HiringActor actor, JobRequisition r) {
        return r.getApprovals().stream()
                .anyMatch(a -> a.getApproverEmployee() != null && a.getApproverEmployee().getId().equals(actor.id()));
    }

    public boolean canViewRequisition(HiringActor actor, JobRequisition r) {
        return actor.staff()
                || isRequisitionOwner(actor, r)
                || (r.getRecruiter() != null && r.getRecruiter().getId().equals(actor.id()))
                || isRequisitionApprover(actor, r);
    }

    public void requireCanViewRequisition(HiringActor actor, JobRequisition r) {
        if (!canViewRequisition(actor, r)) {
            throw new AccessDeniedException("You do not have access to this requisition");
        }
    }

    /**
     * Candidate data is sensitive. Besides hiring staff, only the hiring manager of a requisition
     * the candidate applied to, and interviewers on that candidate's panels, may see the profile.
     */
    public boolean canViewCandidate(HiringActor actor, Long candidateId) {
        return actor.staff()
                || applicationRepository.existsByCandidateIdAndRequisition_HiringManager_Id(candidateId, actor.id())
                || interviewRepository.existsByApplication_Candidate_IdAndPanel_Interviewer_Id(candidateId, actor.id());
    }

    public void requireCanViewCandidate(HiringActor actor, Long candidateId) {
        if (!canViewCandidate(actor, candidateId)) {
            throw new AccessDeniedException("You do not have access to this candidate");
        }
    }

    public boolean canViewApplication(HiringActor actor, JobApplication application) {
        return actor.staff()
                || isRequisitionOwner(actor, application.getRequisition())
                || (application.getRequisition().getRecruiter() != null
                        && application.getRequisition().getRecruiter().getId().equals(actor.id()))
                || interviewRepository.existsByApplication_IdAndPanel_Interviewer_Id(application.getId(), actor.id());
    }

    public void requireCanViewApplication(HiringActor actor, JobApplication application) {
        if (!canViewApplication(actor, application)) {
            throw new AccessDeniedException("You do not have access to this application");
        }
    }

    /** Hiring staff or the hiring manager of the requisition may move candidates through the pipeline. */
    public void requireCanWorkPipeline(HiringActor actor, JobRequisition r) {
        if (!(actor.staff() || r.getHiringManager().getId().equals(actor.id()))) {
            throw new AccessDeniedException("Only recruiters, admins or the hiring manager can move candidates in this requisition");
        }
    }

    private boolean hasAuthority(UserDetails principal, String authority) {
        return principal.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
