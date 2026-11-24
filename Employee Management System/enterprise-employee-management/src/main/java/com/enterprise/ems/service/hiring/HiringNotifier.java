package com.enterprise.ems.service.hiring;

import com.enterprise.ems.entity.Candidate;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.Interview;
import com.enterprise.ems.entity.Offer;
import com.enterprise.ems.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/*
 * PURPOSE: All hiring e-mails in one place. Sending is async and best-effort
 * (EmailServiceImpl logs and skips when mail isn't configured), so a mail problem
 * never blocks or rolls back a hiring action.
 */
@Component
@RequiredArgsConstructor
public class HiringNotifier {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' hh:mm a");

    private final EmailService emailService;

    @Value("${app.hiring.company-name:Our Company}")
    private String companyName;

    public void approvalRequested(Employee approver, String what, String reference, String summary) {
        send(approver, "Approval needed: " + what + " " + reference,
                "Hello " + approver.getFirstName() + ",\n\n"
                        + "A " + what.toLowerCase() + " is waiting for your approval.\n\n"
                        + summary + "\n\n"
                        + "Please sign in to EMS and open Hiring > Approvals to review it.\n\nRegards,\nEEMS Team");
    }

    public void decided(Employee to, String what, String reference, String outcome, String remarks) {
        send(to, what + " " + reference + " " + outcome,
                "Hello " + to.getFirstName() + ",\n\n"
                        + "Your " + what.toLowerCase() + " " + reference + " was " + outcome.toLowerCase() + "."
                        + (remarks != null && !remarks.isBlank() ? "\n\nRemarks: " + remarks : "")
                        + "\n\nRegards,\nEEMS Team");
    }

    public void interviewForPanelist(Employee interviewer, Interview interview, boolean rescheduled) {
        Candidate c = interview.getApplication().getCandidate();
        send(interviewer, (rescheduled ? "Interview rescheduled: " : "Interview scheduled: ") + c.getFullName(),
                "Hello " + interviewer.getFirstName() + ",\n\n"
                        + "You are on the panel for round " + interview.getRoundNumber() + " (" + interview.getRoundName() + ") "
                        + "for " + interview.getApplication().getRequisition().getTitle() + ".\n\n"
                        + "Candidate: " + c.getFullName() + "\n"
                        + "When: " + interview.getScheduledAt().format(WHEN) + " (" + interview.getDurationMinutes() + " min)\n"
                        + "Mode: " + interview.getMode() + "\n"
                        + (interview.getLocationOrLink() != null ? "Where / link: " + interview.getLocationOrLink() + "\n" : "")
                        + "\nPlease submit your feedback in EMS (Hiring > My Interviews) after the interview.\n\nRegards,\nEEMS Team");
    }

    public void interviewForCandidate(Candidate candidate, Interview interview, boolean rescheduled) {
        sendTo(candidate.getEmail(), (rescheduled ? "Interview rescheduled - " : "Interview invitation - ") + companyName,
                "Dear " + candidate.getFullName() + ",\n\n"
                        + "Your interview for " + interview.getApplication().getRequisition().getTitle()
                        + " at " + companyName + " is " + (rescheduled ? "rescheduled" : "scheduled") + ".\n\n"
                        + "Round: " + interview.getRoundName() + "\n"
                        + "When: " + interview.getScheduledAt().format(WHEN) + " (" + interview.getDurationMinutes() + " min)\n"
                        + "Mode: " + interview.getMode() + "\n"
                        + (interview.getLocationOrLink() != null ? "Where / link: " + interview.getLocationOrLink() + "\n" : "")
                        + "\nRegards,\n" + companyName + " Talent Team");
    }

    public void interviewCancelled(Candidate candidate, java.util.Collection<Employee> panel, Interview interview) {
        String when = interview.getScheduledAt().format(WHEN);
        sendTo(candidate.getEmail(), "Interview cancelled - " + companyName,
                "Dear " + candidate.getFullName() + ",\n\nYour interview scheduled for " + when
                        + " has been cancelled. We will contact you with the next steps.\n\nRegards,\n" + companyName + " Talent Team");
        for (Employee e : panel) {
            send(e, "Interview cancelled: " + candidate.getFullName(),
                    "Hello " + e.getFirstName() + ",\n\nThe interview with " + candidate.getFullName() + " scheduled for "
                            + when + " has been cancelled.\n\nRegards,\nEEMS Team");
        }
    }

    public void offerSent(Candidate candidate, Offer offer) {
        sendTo(candidate.getEmail(), "Job offer - " + companyName,
                "Dear " + candidate.getFullName() + ",\n\n"
                        + "We are pleased to offer you the position of " + offer.getDesignation().getName()
                        + " at " + companyName + ".\n\n"
                        + "Proposed joining date: " + offer.getJoiningDate() + "\n"
                        + "This offer is valid until " + offer.getExpiryDate() + ".\n\n"
                        + "Our recruiter will share the detailed offer letter and next steps with you.\n\n"
                        + "Regards,\n" + companyName + " Talent Team");
    }

    private void send(Employee to, String subject, String body) {
        if (to == null) return;
        sendTo(to.getEmail(), subject, body);
    }

    private void sendTo(String email, String subject, String body) {
        if (email == null || email.isBlank()) return;
        try {
            emailService.sendHiringEmail(email, subject, body);
        } catch (RuntimeException ignored) {
            // notification is best effort - never fail a hiring action because of mail
        }
    }
}
