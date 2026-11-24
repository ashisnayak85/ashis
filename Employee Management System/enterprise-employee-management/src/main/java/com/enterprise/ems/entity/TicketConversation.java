package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: The full activity thread for one ticket - plain replies AND every
 * status-changing action, as a single ordered timeline (same idea as a chat
 * app rendering "X was added to the group" inline with real messages,
 * instead of a separate hidden audit log).
 * TABLE: ticket_conversation
 *
 * entryType distinguishes what kind of row this is:
 *   REPLY      - plain back-and-forth comment, either side
 *   CLAIM      - assignedTo accepted the assignment (starts resolution SLA)
 *   RESOLVE    - assignedTo resolved it (message = resolution note)
 *   REJECT     - assignedTo rejected it (message = mandatory reason)
 *   TRANSFER   - assignedTo handed it to targetEmployee
 *   ESCALATE   - raisedBy escalated an unsatisfying RESOLVE/REJECT
 *   ACCEPT     - raisedBy accepted the outcome (auto-closes the ticket)
 *   CLOSE      - raisedBy self-closed without waiting for a resolution
 *   SLA_BREACH - system-generated, written by TicketSlaScheduler
 *
 * IMMUTABLE BY DESIGN: no update/edit endpoint is ever exposed for these rows
 * (unlike WhatsApp's editable messages) - a support/audit trail that can be
 * silently rewritten after the fact isn't trustworthy. See TicketServiceImpl.
 */
@Entity
@Table(name = "ticket_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketMaster ticket;

    // Null for SLA_BREACH rows (system-generated, no human author).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Employee author;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(name = "message", length = 2000)
    private String message;

    // Only set on TRANSFER (who it was handed to) and ESCALATE (which
    // escalation contact/HOD it landed on).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_employee_id")
    private Employee targetEmployee;

    // Optional "replying to this specific earlier message" pointer, for long
    // threads with multiple rounds of back-and-forth (WhatsApp-style quote-reply).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entry_id")
    private TicketConversation parentEntry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
