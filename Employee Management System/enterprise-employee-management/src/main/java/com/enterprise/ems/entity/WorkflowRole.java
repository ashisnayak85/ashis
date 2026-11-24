package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: Admin-managed BUSINESS role master - "who resolves an approval
 * stage", completely separate from the auth `Role` entity ("what can this
 * login do in the system"). Deliberately its own table rather than reusing
 * `roles`/`Role`:
 *
 *   - Role/User (auth)        -> Spring Security permissions, login access
 *   - WorkflowRole (this)     -> business approval routing (HOD, HR Head,
 *                                 Project Head, Zonal Manager, ...)
 *
 * Keeping these separate means HR can grant someone approval authority
 * ("HOD") without also having to grant them elevated login permissions, and
 * vice versa. Admin creates/edits/deactivates rows here entirely through the
 * API - see WorkflowRoleService - no code change needed to add a new
 * business role.
 * TABLE: workflow_role
 */
@Entity
@Table(name = "workflow_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name; // e.g. "HOD", "HR Head", "Project Head" - admin-defined, free text

    @Column(name = "description", length = 255)
    private String description;

    // Deactivating (rather than deleting) preserves history for anything
    // already resolved/frozen against this role (e.g. ResignationApproval
    // rows, or a stage that referenced it) - same pattern as
    // WorkflowStage.active / Department.active elsewhere in this
    // codebase.
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
