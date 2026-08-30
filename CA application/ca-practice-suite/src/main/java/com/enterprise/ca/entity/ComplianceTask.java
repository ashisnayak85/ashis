package com.enterprise.ca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * PURPOSE: The compliance calendar - the single most valuable thing a CA
 * tracks: what statutory filing is due, for which client, and by when.
 * TicketSlaScheduler's equivalent (ComplianceReminderScheduler) flips PENDING
 * tasks to OVERDUE past their due date, and - for recurring task types -
 * automatically creates next period's task once this one is FILED.
 */
@Entity
@Table(name = "compliance_task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    // Nullable: some tasks are firm-wide (e.g. internal audit prep), not
    // tied to one client.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(length = 1000)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TaskType {
        GSTR1, GSTR3B, GST_ANNUAL_RETURN, TDS_RETURN_24Q, TDS_RETURN_26Q,
        ADVANCE_TAX, INCOME_TAX_RETURN, TAX_AUDIT, ROC_ANNUAL_FILING,
        PF_ESI_RETURN, PROFESSIONAL_TAX, CUSTOM
    }

    public enum Frequency {
        MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY, ONE_TIME
    }

    public enum TaskStatus {
        PENDING, IN_PROGRESS, FILED, OVERDUE
    }
}
