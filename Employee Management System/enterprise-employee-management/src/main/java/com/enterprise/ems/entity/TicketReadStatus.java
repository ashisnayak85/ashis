package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: "How far has this employee read into this ticket's conversation" -
 * one row per (ticket, employee). Comparing lastReadAt against the ticket's
 * newest conversation entry drives the unread badge (see TicketServiceImpl)
 * without needing a separate flag on every single conversation row.
 * TABLE: ticket_read_status
 */
@Entity
@Table(name = "ticket_read_status", uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketMaster ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;
}
