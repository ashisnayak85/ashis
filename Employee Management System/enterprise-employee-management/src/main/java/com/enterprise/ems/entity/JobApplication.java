package com.enterprise.ems.entity;

import com.enterprise.ems.constant.ApplicationStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * PURPOSE: One candidate applying to one requisition - the object that
 * actually moves through the hiring pipeline. Every move is recorded in
 * ApplicationStageHistory.
 * TABLE: job_application  (unique per candidate + requisition)
 */
@Entity
@Table(name = "job_application",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_candidate_requisition",
                columnNames = {"candidate_id", "requisition_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private JobRequisition requisition;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stage", nullable = false, length = 20)
    @Builder.Default
    private ApplicationStage stage = ApplicationStage.APPLIED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "applied_on", nullable = false)
    private LocalDate appliedOn;

    @Column(name = "stage_changed_at", nullable = false)
    private LocalDateTime stageChangedAt;

    // Set when an accepted offer is converted into an Employee.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hired_employee_id")
    private Employee hiredEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Employee createdBy;

    // Two recruiters can't move the same candidate at the same moment.
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
