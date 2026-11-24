package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: No-dues clearance - one row per department that must sign off
 * before final approval (IT clears access/laptop, Finance clears
 * advances/loans, Admin clears asset return, HR clears final settlement).
 * Deliberately NOT a single flat "no dues done" boolean on Resignation:
 * each department clears independently, on its own timeline, with its own
 * remarks - collapsing that into one field would need reworking the moment
 * a second clearing department shows up.
 * TABLE: resignation_clearance
 *
 * Which departments are required is resolved the same way approval stages
 * are (a small admin-configurable list), not hardcoded - kept out of scope
 * for this first pass since the immediate need is IT + Assets + Finance.
 */
@Entity
@Table(name = "resignation_clearance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationClearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resignation_id", nullable = false)
    private Resignation resignation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // PENDING / CLEARED / BLOCKED - BLOCKED lets a department flag "cannot
    // clear yet" (e.g. asset not returned) distinctly from just not-yet-acted
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_by_employee_id")
    private Employee clearedBy;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;
}
