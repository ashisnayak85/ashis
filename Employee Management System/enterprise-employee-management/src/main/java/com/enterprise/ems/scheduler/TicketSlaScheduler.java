package com.enterprise.ems.scheduler;

import com.enterprise.ems.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Runs TicketService.checkSlaBreaches() on a timer so "not accepted
 * in time" / "not resolved in time" gets flagged and recorded even if nobody
 * happens to open the ticket - see the acceptance/resolution SLA discussion
 * in TicketServiceImpl's class comment.
 *
 * Records breaches only; does NOT auto-escalate a breached ticket. That's a
 * deliberate, separate decision - flip this into calling ticketService's
 * escalate-on-breach logic later if the business wants that instead.
 */
@Component
@RequiredArgsConstructor
public class TicketSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketSlaScheduler.class);

    private final TicketService ticketService;

    // Every 15 minutes - frequent enough that a breach shows up promptly
    // without hammering the DB with a full-table scan every few seconds.
    @Scheduled(fixedRate = 900000)
    public void run() {
        try {
            ticketService.checkSlaBreaches();
        } catch (Exception e) {
            log.error("Ticket SLA breach check failed", e);
        }
    }
}
