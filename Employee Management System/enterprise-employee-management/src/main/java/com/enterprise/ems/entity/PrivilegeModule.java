package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: Master list of modules that can have their own admin-configured
 * approval workflow (RESIGNATION, LEAVE, EXPENSE, ASSET_REQUEST, ...).
 * TABLE: privilege_module
 *
 * WHY THIS EXISTS: before this table, every new approval-driven module
 * (resignation, and eventually leave/expense/...) needed its own
 * XxxApprovalStage table, its own admin screen, and its own sidebar entry.
 * That doesn't scale - N modules would mean N near-duplicate screens.
 * WorkflowStage now has a privilegeModuleId, so ONE admin screen
 * ("Workflow Setup") with a module dropdown covers every module, and
 * WorkflowRole / EmployeeWorkflowRole (already module-agnostic) are shared
 * across all of them without any change.
 *
 * OWNERSHIP SPLIT (intentional, don't blur this):
 *   - Rows in THIS table are added when a new module is actually BUILT
 *     (its own entity/service/state-machine exists to consume a workflow) -
 *     that's a real development event, so it's fine for this to be admin-
 *     creatable but is expected to be rare and coordinated with engineering.
 *   - Everything that hangs off a module (its WorkflowStage chain, which
 *     WorkflowRole holds what, via EmployeeWorkflowRole) is 100%
 *     admin-operated day to day, same as before - no code change needed to
 *     add a stage or reassign a role on an EXISTING module.
 */
@Entity
@Table(name = "privilege_module")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivilegeModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stable machine key referenced from code, e.g. AppConstants.MODULE_CODE_RESIGNATION.
    // Uppercase, underscore-separated by convention - not enforced at the DB level.
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // "RESIGNATION", "LEAVE", "EXPENSE", ...

    @Column(name = "name", nullable = false, length = 100)
    private String name; // "Resignation", "Leave Request" - shown in the admin dropdown

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
