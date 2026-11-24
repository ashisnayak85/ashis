package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: One row per approval-stage instance actually created for a given
 * resignation - resolved from WorkflowStage (the policy) at
 * submit time and then frozen here. This is the audit trail: "who was asked,
 * what did they decide, when".
 * TABLE: resignation_approval
 *
 * RESOLVED, NOT LIVE: approverEmployee is captured once at submit time (who
 * WAS the reporting manager / role-holder right then), not re-resolved on
 * every read. If the manager changes mid-flight, this row still shows who
 * was actually asked - matches how TicketMaster freezes assignedTo rather
 * than re-deriving it.
 *
 * sequenceOrder is denormalized from the source stage so history stays
 * correct even if WorkflowStage rows are later reordered/edited.
 */
@Entity
@Table(name = "resignation_approval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resignation_id", nullable = false)
    private Resignation resignation;

    // The policy row this instance was resolved from - kept for traceability,
    // not relied on for ordering (see sequenceOrder below).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private WorkflowStage stage;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName; // denormalized snapshot of stage.name

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder; // denormalized snapshot of stage.sequenceOrder

    // Resolved at submit time - see WorkflowStage.approverType
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_employee_id", nullable = false)
    private Employee approverEmployee;

    // PENDING / APPROVED / REJECTED / SKIPPED
    // SKIPPED covers "resolver found nobody eligible" or an admin manually
    // bypassing a stage - never leave a stage silently stuck.
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;
}
