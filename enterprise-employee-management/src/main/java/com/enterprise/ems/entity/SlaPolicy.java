package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

/*
 * PURPOSE: How many hours a department is allowed for a ticket of a given
 * priority before it's flagged as an acceptance/resolution SLA breach.
 * TABLE: sla_policy
 *
 * One row per (department, priority). A HIGH priority IT ticket should have
 * a tighter clock than a LOW priority Facilities ticket - hardcoding a single
 * global threshold would get that wrong. If no row exists for a given
 * department+priority, TicketServiceImpl falls back to a sane built-in
 * default (see TicketServiceImpl.DEFAULT_SLA_HOURS) so the feature works
 * out of the box before an admin configures anything.
 */
@Entity
@Table(name = "sla_policy", uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "priority"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // LOW / MEDIUM / HIGH
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    // How long the ticket can sit unclaimed before it's an acceptance breach.
    @Column(name = "acceptance_hours", nullable = false)
    private Integer acceptanceHours;

    // How long the responsible person has to resolve/reject after accepting,
    // before it's a resolution breach.
    @Column(name = "resolution_hours", nullable = false)
    private Integer resolutionHours;
}
