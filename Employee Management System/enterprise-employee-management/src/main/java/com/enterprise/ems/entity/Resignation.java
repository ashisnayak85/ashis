package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE: One row per resignation request - the "header" of the
 * resignation / resignation_approval / resignation_clearance trio, mirroring
 * how TicketMaster is the header for TicketConversation. All overall state
 * (status, dates) lives here; per-stage approval history lives in
 * ResignationApproval, and no-dues clearance lives in ResignationClearance.
 * TABLE: resignation
 *
 * STATUS STATE MACHINE (see ResignationServiceImpl for actual transitions):
 *   SUBMITTED           -> raised by employee, first approval stage pending
 *   IN_APPROVAL         -> at least one approval stage done, more remain
 *   ALL_APPROVALS_DONE   -> every WorkflowStage cleared, no-dues not yet started
 *   CLEARANCE_PENDING    -> no-dues in progress (see ResignationClearance)
 *   CLEARANCE_DONE        -> every clearance department has signed off
 *   FINAL_APPROVED        -> HR final approval done; employee.active set false,
 *                            login disabled, this is the terminal "left" state
 *   REJECTED              -> any stage rejected it; terminal
 *   WITHDRAWN             -> employee withdrew before FINAL_APPROVED; terminal
 *
 * Two dates on purpose (see chat discussion): expectedLastWorkingDay is
 * system-calculated from noticePeriodDays at submit time; actualLastWorkingDay
 * is only set once HR/manager overrides it (buyout, immediate relieving,
 * extension) - keep both so "what was promised" vs "what actually happened"
 * is never lost.
 */
@Entity
@Table(name = "resignation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resignation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;

    // Snapshot of the policy value at submit time (from designation/company
    // policy) - kept even if the policy later changes, so old resignations
    // don't silently recalculate.
    @Column(name = "notice_period_days", nullable = false)
    private Integer noticePeriodDays;

    // System-calculated: requestedOn + noticePeriodDays
    @Column(name = "expected_last_working_day", nullable = false)
    private LocalDate expectedLastWorkingDay;

    // Only set once HR/manager overrides the expected date (buyout, early
    // relieving, extension). Null means "expected date still stands".
    @Column(name = "actual_last_working_day")
    private LocalDate actualLastWorkingDay;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "SUBMITTED";

    @OneToMany(mappedBy = "resignation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sequenceOrder ASC")
    private List<ResignationApproval> approvals = new ArrayList<>();

    @OneToMany(mappedBy = "resignation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResignationClearance> clearances = new ArrayList<>();

    @OneToMany(mappedBy = "resignation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResignationAsset> assets = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
