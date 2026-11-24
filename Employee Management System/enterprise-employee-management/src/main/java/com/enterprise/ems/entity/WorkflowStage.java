package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: Configurable approval-chain POLICY for a given PrivilegeModule -
 * admin-managed, reused by every workflow instance of that module while
 * active. This table answers "what stages exist and who resolves each one,
 * for THIS module", NOT "what happened on this particular request" (that's
 * still ResignationApproval for the resignation module today).
 * TABLE: workflow_stage
 *
 * FORMERLY: this table was "resignation_approval_stage" and had no module
 * concept - it only ever served the resignation module. Renamed and scoped
 * by privilegeModule so any future module (leave, expense, ...) reuses this
 * SAME table and the SAME admin screen instead of getting its own copy and
 * its own sidebar entry. See PrivilegeModule entity javadoc for the full
 * reasoning.
 *
 * sequenceOrder is unique PER MODULE (not globally) - see the composite
 * unique constraint in the DDL - so "stage 1" of Resignation and "stage 1"
 * of Leave don't collide.
 *
 * approverType drives how the owning module's service resolves the
 * *actual* approver for a given employee at submit time (see
 * ResignationServiceImpl.resolveApprover() for the current, only consumer):
 *   REPORTING_MANAGER  -> employee.reportingManager
 *   ROLE_IN_DEPARTMENT -> any employee holding `workflowRole` (via
 *                          EmployeeWorkflowRole) within the employee's own
 *                          department (or fixedDepartment, if scoped there)
 *   SPECIFIC_ROLE      -> any employee holding `workflowRole` company-wide
 *   SPECIFIC_USER      -> hardcoded fallback employee, use sparingly
 *
 * departmentScope only matters for ROLE_IN_DEPARTMENT:
 *   EMPLOYEE_DEPARTMENT -> resolve within the resigning/requesting employee's own department
 *   FIXED_DEPARTMENT    -> always resolve within fixedDepartment regardless of
 *                          who's requesting (e.g. an HR stage always routes to
 *                          the HR department's role-holder)
 *
 * `workflowRole` points at WorkflowRole (admin-managed business role),
 * never the auth Role - see WorkflowRole entity javadoc.
 */
@Entity
@Table(name = "workflow_stage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which module this stage belongs to - "RESIGNATION", "LEAVE", etc.
    // See PrivilegeModule entity javadoc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_module_id", nullable = false)
    private PrivilegeModule privilegeModule;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // e.g. "Reporting Manager", "HOD", "HR Final"

    // Enforces ordering within its module - a stage can only be actioned
    // once every stage with a lower sequenceOrder (in the SAME module) is
    // APPROVED. Unique per-module (see DDL composite unique constraint) so
    // two stages in the same module can't tie, while different modules
    // freely reuse the same numbers.
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    // REPORTING_MANAGER / ROLE_IN_DEPARTMENT / SPECIFIC_ROLE / SPECIFIC_USER
    @Column(name = "approver_type", nullable = false, length = 30)
    private String approverType;

    // Used when approverType = ROLE_IN_DEPARTMENT or SPECIFIC_ROLE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_role_id")
    private WorkflowRole workflowRole;

    // EMPLOYEE_DEPARTMENT / FIXED_DEPARTMENT - only read when
    // approverType = ROLE_IN_DEPARTMENT
    @Column(name = "department_scope", length = 30)
    private String departmentScope;

    // Used when departmentScope = FIXED_DEPARTMENT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_department_id")
    private Department fixedDepartment;

    // Used when approverType = SPECIFIC_USER - hardcoded fallback approver
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specific_employee_id")
    private Employee specificEmployee;

    // Deactivating (rather than deleting) preserves history for workflow
    // instances already mid-flight against this stage.
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
