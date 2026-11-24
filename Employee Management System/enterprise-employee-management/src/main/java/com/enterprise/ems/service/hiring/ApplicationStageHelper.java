package com.enterprise.ems.service.hiring;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.entity.ApplicationStageHistory;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.JobApplication;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.repository.ApplicationStageHistoryRepository;
import com.enterprise.ems.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * PURPOSE: The ONE place an application's stage is changed. Application, Interview and
 * Offer services all go through here, so the transition rules (ApplicationStage#canMoveTo)
 * are enforced and a history row is written for every change - no matter which flow caused it.
 */
@Component
@RequiredArgsConstructor
public class ApplicationStageHelper {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository historyRepository;

    /** First history row for a brand-new application (from = null). */
    public void recordCreated(JobApplication application, Employee by) {
        historyRepository.save(ApplicationStageHistory.builder()
                .application(application)
                .fromStage(null)
                .toStage(application.getStage())
                .changedBy(by)
                .changedAt(LocalDateTime.now())
                .remarks("Application created")
                .build());
    }

    public void transition(JobApplication application, ApplicationStage to, Employee by, String remarks) {
        ApplicationStage from = application.getStage();
        if (!from.canMoveTo(to)) {
            throw new BusinessException("An application in stage " + from + " cannot be moved to " + to);
        }
        application.setStage(to);
        application.setStageChangedAt(LocalDateTime.now());
        if (to == ApplicationStage.REJECTED || to == ApplicationStage.WITHDRAWN) {
            application.setRejectionReason(remarks);
        } else if (from == ApplicationStage.REJECTED) {
            application.setRejectionReason(null); // re-opened
        }
        applicationRepository.save(application);

        historyRepository.save(ApplicationStageHistory.builder()
                .application(application)
                .fromStage(from)
                .toStage(to)
                .changedBy(by)
                .changedAt(LocalDateTime.now())
                .remarks(remarks)
                .build());
    }
}
