package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.constant.InterviewStatus;
import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.dto.HiringRequests.FeedbackRequest;
import com.enterprise.ems.dto.InterviewDTO;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.HiringMapper;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.InterviewRepository;
import com.enterprise.ems.repository.JobApplicationRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.InterviewService;
import com.enterprise.ems.service.hiring.ApplicationStageHelper;
import com.enterprise.ems.service.hiring.HiringAccessService;
import com.enterprise.ems.service.hiring.HiringActor;
import com.enterprise.ems.service.hiring.HiringNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/*
 * PURPOSE: Interview rounds - schedule, reschedule, cancel, and collect feedback from
 * each panelist. Feedback is blind between panelists (see HiringMapper#toDTO(Interview, ...)).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private static final int MAX_PANEL_SIZE = 10;

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationStageHelper stageHelper;
    private final HiringAccessService access;
    private final HiringMapper mapper;
    private final HiringNotifier notifier;
    private final AuditService auditService;

    @Override
    public InterviewDTO schedule(Long applicationId, InterviewDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        if (application.getStage() != ApplicationStage.SCREENING && application.getStage() != ApplicationStage.INTERVIEW) {
            throw new BusinessException("Interviews can be scheduled only for applications in SCREENING or INTERVIEW stage (current: "
                    + application.getStage() + ")");
        }
        if (application.getRequisition().getStatus() != RequisitionStatus.OPEN) {
            throw new BusinessException("The requisition is " + application.getRequisition().getStatus() + " - interviews can only be scheduled while it is OPEN");
        }
        requireNotInPast(dto.getScheduledAt());
        List<Employee> panel = resolvePanel(dto.getInterviewerIds(), application.getCandidate());

        Integer max = interviewRepository.findMaxRoundNumber(applicationId);
        Interview interview = Interview.builder()
                .application(application)
                .roundNumber(max == null ? 1 : max + 1)
                .roundName(dto.getRoundName().trim())
                .mode(dto.getMode())
                .scheduledAt(dto.getScheduledAt())
                .durationMinutes(dto.getDurationMinutes())
                .locationOrLink(blankToNull(dto.getLocationOrLink()))
                .status(InterviewStatus.SCHEDULED)
                .createdBy(actor.employee())
                .build();
        for (Employee e : panel) {
            interview.getPanel().add(InterviewPanelist.builder().interview(interview).interviewer(e).build());
        }
        interview = interviewRepository.save(interview);

        if (application.getStage() == ApplicationStage.SCREENING) {
            stageHelper.transition(application, ApplicationStage.INTERVIEW, actor.employee(),
                    "Interview round " + interview.getRoundNumber() + " scheduled");
        }

        for (Employee e : panel) {
            notifier.interviewForPanelist(e, interview, false);
        }
        notifier.interviewForCandidate(application.getCandidate(), interview, false);

        auditService.log("SCHEDULE_INTERVIEW", "Interview", interview.getId(),
                "Round " + interview.getRoundNumber() + " for " + application.getCandidate().getFullName() + " at " + interview.getScheduledAt());
        return mapper.toDTO(interview, actor);
    }

    @Override
    public InterviewDTO update(Long id, InterviewDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        Interview interview = getOrThrow(id);
        requireScheduled(interview, "changed");
        requireNotInPast(dto.getScheduledAt());

        List<Employee> wanted = resolvePanel(dto.getInterviewerIds(), interview.getApplication().getCandidate());
        Set<Long> wantedIds = new HashSet<>();
        wanted.forEach(e -> wantedIds.add(e.getId()));

        // A panelist who has already given feedback can't be dropped - that would silently delete their evaluation.
        for (InterviewPanelist p : interview.getPanel()) {
            if (!wantedIds.contains(p.getInterviewer().getId()) && p.isSubmitted()) {
                throw new BusinessException(p.getInterviewer().getFullName() + " has already submitted feedback and cannot be removed from the panel");
            }
        }

        boolean scheduleChanged = !interview.getScheduledAt().equals(dto.getScheduledAt())
                || interview.getMode() != dto.getMode()
                || !Objects.equals(interview.getDurationMinutes(), dto.getDurationMinutes())
                || !Objects.equals(interview.getLocationOrLink(), blankToNull(dto.getLocationOrLink()));

        interview.setRoundName(dto.getRoundName().trim());
        interview.setMode(dto.getMode());
        interview.setScheduledAt(dto.getScheduledAt());
        interview.setDurationMinutes(dto.getDurationMinutes());
        interview.setLocationOrLink(blankToNull(dto.getLocationOrLink()));

        interview.getPanel().removeIf(p -> !wantedIds.contains(p.getInterviewer().getId()));
        Set<Long> existingIds = new HashSet<>();
        interview.getPanel().forEach(p -> existingIds.add(p.getInterviewer().getId()));
        List<Employee> added = new ArrayList<>();
        for (Employee e : wanted) {
            if (!existingIds.contains(e.getId())) {
                interview.getPanel().add(InterviewPanelist.builder().interview(interview).interviewer(e).build());
                added.add(e);
            }
        }
        interviewRepository.save(interview);

        for (InterviewPanelist p : interview.getPanel()) {
            boolean isNew = added.stream().anyMatch(e -> e.getId().equals(p.getInterviewer().getId()));
            if (isNew) {
                notifier.interviewForPanelist(p.getInterviewer(), interview, false);
            } else if (scheduleChanged) {
                notifier.interviewForPanelist(p.getInterviewer(), interview, true);
            }
        }
        if (scheduleChanged) {
            notifier.interviewForCandidate(interview.getApplication().getCandidate(), interview, true);
        }

        auditService.log("UPDATE_INTERVIEW", "Interview", interview.getId(), "Interview round " + interview.getRoundNumber() + " updated");
        return mapper.toDTO(interview, actor);
    }

    @Override
    public InterviewDTO cancel(Long id, String reason, HiringActor actor) {
        access.requireStaff(actor);
        Interview interview = getOrThrow(id);
        requireScheduled(interview, "cancelled");
        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancelReason(blankToNull(reason));
        interviewRepository.save(interview);

        List<Employee> panel = interview.getPanel().stream().map(InterviewPanelist::getInterviewer).toList();
        notifier.interviewCancelled(interview.getApplication().getCandidate(), panel, interview);

        auditService.log("CANCEL_INTERVIEW", "Interview", interview.getId(), "Interview round " + interview.getRoundNumber() + " cancelled");
        return mapper.toDTO(interview, actor);
    }

    @Override
    public InterviewDTO markStatus(Long id, InterviewStatus status, HiringActor actor) {
        access.requireStaff(actor);
        if (status != InterviewStatus.COMPLETED && status != InterviewStatus.NO_SHOW) {
            throw new BusinessException("Only COMPLETED or NO_SHOW can be set manually (use cancel to cancel an interview)");
        }
        Interview interview = getOrThrow(id);
        requireScheduled(interview, "updated");
        interview.setStatus(status);
        interviewRepository.save(interview);
        auditService.log("INTERVIEW_STATUS", "Interview", interview.getId(), "Interview round " + interview.getRoundNumber() + " marked " + status);
        return mapper.toDTO(interview, actor);
    }

    @Override
    public InterviewDTO submitFeedback(Long id, FeedbackRequest request, HiringActor actor) {
        Interview interview = getOrThrow(id);
        InterviewPanelist mine = interview.getPanel().stream()
                .filter(p -> p.getInterviewer().getId().equals(actor.id()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("You are not on the panel of this interview"));

        if (interview.getStatus() == InterviewStatus.CANCELLED || interview.getStatus() == InterviewStatus.NO_SHOW) {
            throw new BusinessException("Feedback cannot be submitted for a " + interview.getStatus() + " interview");
        }

        mine.setRating(request.rating());
        mine.setRecommendation(request.recommendation());
        mine.setComments(blankToNull(request.comments()));
        mine.setSubmittedAt(LocalDateTime.now());

        // Once everyone on the panel has submitted, the round is complete.
        boolean allIn = interview.getPanel().stream().allMatch(InterviewPanelist::isSubmitted);
        if (allIn && interview.getStatus() == InterviewStatus.SCHEDULED) {
            interview.setStatus(InterviewStatus.COMPLETED);
        }
        interviewRepository.save(interview);

        auditService.log("INTERVIEW_FEEDBACK", "Interview", interview.getId(),
                "Feedback submitted by " + actor.employee().getFullName() + " for round " + interview.getRoundNumber());
        return mapper.toDTO(interview, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewDTO getById(Long id, HiringActor actor) {
        Interview interview = getOrThrow(id);
        requireCanView(interview, actor);
        return mapper.toDTO(interview, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDTO> listByApplication(Long applicationId, HiringActor actor) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        boolean seesAll = actor.staff() || application.getRequisition().getHiringManager().getId().equals(actor.id());

        List<Interview> interviews = interviewRepository.findByApplicationIdOrderByRoundNumberAsc(applicationId);
        List<Interview> visible = seesAll
                ? interviews
                : interviews.stream().filter(i -> isOnPanel(i, actor)).toList();
        if (!seesAll && visible.isEmpty()) {
            throw new AccessDeniedException("You do not have access to this application's interviews");
        }
        return visible.stream().map(i -> mapper.toDTO(i, actor)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDTO> myInterviews(HiringActor actor) {
        return interviewRepository.findForInterviewer(actor.id()).stream()
                .map(i -> mapper.toDTO(i, actor))
                .toList();
    }

    // ============================================================ helpers

    private Interview getOrThrow(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + id));
    }

    private boolean isOnPanel(Interview i, HiringActor actor) {
        return i.getPanel().stream().anyMatch(p -> p.getInterviewer().getId().equals(actor.id()));
    }

    private void requireCanView(Interview i, HiringActor actor) {
        boolean hiringManager = i.getApplication().getRequisition().getHiringManager().getId().equals(actor.id());
        if (!(actor.staff() || hiringManager || isOnPanel(i, actor))) {
            throw new AccessDeniedException("You do not have access to this interview");
        }
    }

    private void requireScheduled(Interview i, String verb) {
        if (i.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BusinessException("Only a SCHEDULED interview can be " + verb + " (current status: " + i.getStatus() + ")");
        }
    }

    // A few minutes of slack so "schedule for right now" works; anything clearly in the past is almost certainly a typo.
    private void requireNotInPast(LocalDateTime when) {
        if (when.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException("The interview date and time cannot be in the past");
        }
    }

    private List<Employee> resolvePanel(List<Long> ids, Candidate candidate) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Add at least one interviewer to the panel");
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() > MAX_PANEL_SIZE) {
            throw new BusinessException("A panel can have at most " + MAX_PANEL_SIZE + " interviewers");
        }
        List<Employee> panel = new ArrayList<>();
        for (Long empId : unique) {
            Employee e = employeeRepository.findById(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found: " + empId));
            if (!Boolean.TRUE.equals(e.getActive())) {
                throw new BusinessException(e.getFullName() + " is not an active employee and cannot be on a panel");
            }
            if (e.getEmail() != null && e.getEmail().equalsIgnoreCase(candidate.getEmail())) {
                throw new BusinessException("A candidate cannot interview themselves");
            }
            panel.add(e);
        }
        return panel;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
