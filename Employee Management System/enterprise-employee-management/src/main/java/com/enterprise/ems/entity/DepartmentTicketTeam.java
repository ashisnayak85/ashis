package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: Who can handle tickets for a department, and in what capacity.
 * TABLE: department_ticket_team
 *
 * WHY THIS SHAPE (not two hardcoded columns on Department):
 * A flat "responsible_person_id" + "escalation_members" design bottlenecks on
 * one person and can't grow into multi-level escalation later. This join
 * table lets any number of employees be MEMBERs (the pool a ticket can be
 * claimed from / transferred to) or ESCALATION contacts (where an unhappy
 * user's escalation lands) for a given department, and new rows can be
 * added/removed with zero schema change.
 */
@Entity
@Table(name = "department_ticket_team")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentTicketTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // MEMBER = can claim/be transferred tickets for this department.
    // ESCALATION = where escalate() reassigns an unsatisfied ticket.
    @Column(name = "role_in_team", nullable = false, length = 20)
    private String roleInTeam;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
