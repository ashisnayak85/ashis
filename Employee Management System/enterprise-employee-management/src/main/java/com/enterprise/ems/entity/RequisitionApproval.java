package com.enterprise.ems.entity;

import com.enterprise.ems.constant.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/*
 * PURPOSE: One frozen approval stage of a requisition - same idea as
 * ResignationApproval: resolved from WorkflowStage (the policy) at submit
 * time and then kept as the audit trail of who was asked and what they decided.
 * TABLE: requisition_approval
 *
 * approverEmployee is NULLABLE here (unlike ResignationApproval) because a
 * SKIPPED stage - "nobody eligible was found" - has no approver.
 */
@Entity
@Table(name = "requisition_approval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private JobRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private WorkflowStage stage;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_employee_id")
    private Employee approverEmployee;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;
}
