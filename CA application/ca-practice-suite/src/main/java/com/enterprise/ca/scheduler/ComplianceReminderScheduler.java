package com.enterprise.ca.scheduler;

import com.enterprise.ca.entity.ComplianceTask;
import com.enterprise.ca.repository.ComplianceTaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/*
 * PURPOSE: Runs once a day (see compliance.overdue-check-cron) and flips any
 * PENDING/IN_PROGRESS task whose due date has passed to OVERDUE, so the
 * dashboard's "overdue" count and the compliance list are always accurate
 * without anyone having to manually review dates.
 */
@Component
@RequiredArgsConstructor
public class ComplianceReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReminderScheduler.class);

    private final ComplianceTaskRepository complianceTaskRepository;

    @Scheduled(cron = "${compliance.overdue-check-cron:0 0 6 * * *}")
    public void markOverdueTasks() {
        LocalDate today = LocalDate.now();
        List<ComplianceTask> overdue = complianceTaskRepository.findByStatusAndDueDateBefore(ComplianceTask.TaskStatus.PENDING, today);
        overdue.addAll(complianceTaskRepository.findByStatusAndDueDateBefore(ComplianceTask.TaskStatus.IN_PROGRESS, today));
        for (ComplianceTask task : overdue) {
            task.setStatus(ComplianceTask.TaskStatus.OVERDUE);
        }
        if (!overdue.isEmpty()) {
            complianceTaskRepository.saveAll(overdue);
            log.info("Marked {} compliance task(s) as OVERDUE", overdue.size());
        }
    }
}
