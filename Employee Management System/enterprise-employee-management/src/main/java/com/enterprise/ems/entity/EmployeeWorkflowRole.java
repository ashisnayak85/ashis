package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: "Employee Access" - one row per (employee holds workflowRole,
 * optionally scoped to a department). This is the admin-operated table that
 * answers "who currently holds the HOD role in Finance" etc, and is what
 * ResignationServiceImpl.resolveApprover() queries for approverType
 * ROLE_IN_DEPARTMENT / SPECIFIC_ROLE - see that class.
 * TABLE: employee_workflow_role
 *
 * SCOPE:
 *   scopeDepartment == null  -> company-wide / global hold of this role
 *                                (used for SPECIFIC_ROLE-style stages, e.g.
 *                                a single company-wide "HR Head")
 *   scopeDepartment != null  -> role is held only within that department
 *                                (used for ROLE_IN_DEPARTMENT-style stages,
 *                                e.g. "HOD of Finance")
 *
 * Multiple employees CAN hold the same (workflowRole, scopeDepartment) pair
 * at once (e.g. two people share "HOD" during a handover) - resolution picks
 * the lowest-id active holder, same tie-break EmployeeRepository already used
 * for the auth-role-based lookup this replaces.
 *
 * REVOKE, DON'T DELETE: admin "removing a role from an employee" sets
 * active=false and stamps removedAt/removedBy rather than deleting the row -
 * this keeps a full history of who held what role and when, independent of
 * anything already frozen onto a ResignationApproval elsewhere.
 */
@Entity
@Table(name = "employee_workflow_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWorkflowRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_role_id", nullable = false)
    private WorkflowRole workflowRole;

    // Null = global/company-wide hold of this role. Non-null = scoped to
    // that one department only - see class javadoc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_department_id")
    private Department scopeDepartment;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_employee_id")
    private Employee assignedBy;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by_employee_id")
    private Employee removedBy;
}
