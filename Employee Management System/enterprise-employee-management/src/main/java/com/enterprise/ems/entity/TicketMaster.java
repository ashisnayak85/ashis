package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: One row per raised ticket - the "master" half of the
 * ticket_master / ticket_conversation pair. All state (status, assignment,
 * SLA clocks) lives here; the back-and-forth thread lives in
 * TicketConversation, which references this by ticket_id.
 * TABLE: ticket_master
 *
 * STATUS STATE MACHINE (see TicketServiceImpl for the actual transitions):
 *   OPEN        -> raised, or just (re)assigned and awaiting claim/re-claim
 *   IN_PROGRESS -> claimed by assignedTo, resolution clock running
 *   RESOLVED    -> assignedTo resolved it, awaiting raisedBy's accept/escalate
 *   REJECTED    -> assignedTo rejected it, awaiting raisedBy's accept/escalate
 *   CLOSED      -> terminal (raisedBy accepted the outcome, or self-closed)
 * transfer() and escalate() both send the ticket back to OPEN (new assignee
 * must claim it fresh) rather than being separate statuses of their own -
 * TRANSFER/ESCALATE are recorded as conversation entries instead.
 *
 * TWO SEPARATE SLA CLOCKS (acceptance vs resolution) ON PURPOSE - see
 * TicketServiceImpl's SLA comment for why they're not one timestamp.
 */
@Entity
@Table(name = "ticket_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-friendly identifier e.g. "TKT-000042" - assigned right after the
    // first save once the numeric id is known (see TicketServiceImpl.create).
    @Column(name = "ticket_number", unique = true, length = 20)
    private String ticketNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by", nullable = false)
    private Employee raisedBy;

    // Null until someone (or the system, on transfer/escalate) assigns it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Employee assignedTo;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    // LOW / MEDIUM / HIGH
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    // Bumped every time escalate() runs - lets the UI show "Escalated x2" and
    // lets escalate() detect "we're already at the HOD, nowhere further to go".
    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private Integer escalationLevel = 0;

    // --- SLA clocks ---
    // Acceptance clock: assignedAt -> acceptedAt (was it picked up in time?)
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    // Resolution clock: acceptedAt -> resolvedAt (was it actually solved in time?)
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "acceptance_breached", nullable = false)
    @Builder.Default
    private Boolean acceptanceBreached = false;

    @Column(name = "resolution_breached", nullable = false)
    @Builder.Default
    private Boolean resolutionBreached = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
