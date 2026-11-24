package com.enterprise.ems.scheduler;

import com.enterprise.ems.service.AttendanceService;
import com.enterprise.ems.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * ================================================================================
 * PURPOSE: Scheduler Module (Phase 11)
 * ================================================================================
 * ANNOTATION: @Scheduled - runs method on cron/fixed rate schedule
 * ANNOTATION: @Component - Spring bean, required for @Scheduled to work
 *
 * CRON FORMAT: second minute hour day month weekday
 * "0 0 8 * * *" = Every day at 8:00 AM
 * ================================================================================
 */
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final AttendanceService attendanceService;
    //private final EmailService emailService;

    // Daily attendance report at 8:00 AM
    @Scheduled(cron = "${app.scheduler.daily-report:0 0 8 * * *}")
    public void generateDailyAttendanceReport() {
        log.info("=== DAILY ATTENDANCE REPORT - {} ===", LocalDate.now());
        long presentCount = attendanceService.countPresentToday();
        log.info("Employees present today: {}", presentCount);
        // In production: generate PDF and email to managers
    }

    // Monthly summary on 1st of every month at 9:00 AM
    @Scheduled(cron = "${app.scheduler.monthly-report:0 0 9 1 * *}")
    public void generateMonthlySummaryReport() {
        log.info("=== MONTHLY SUMMARY REPORT - {} ===",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        // In production: aggregate monthly stats and email
    }

    // Health check every 5 minutes (demonstration of fixed rate)
    @Scheduled(fixedRate = 300000)
    public void healthCheck() {
        log.debug("Scheduler health check at {}", LocalDateTime.now());
    }
}
