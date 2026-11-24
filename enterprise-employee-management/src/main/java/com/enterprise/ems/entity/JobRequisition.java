package com.enterprise.ems.entity;

import com.enterprise.ems.constant.RequisitionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE: A request to hire for one or more openings ("manpower request").
 * It is approved through the admin-configured REQUISITION workflow (see
 * RequisitionApproval) and, once OPEN, candidates can be applied to it.
 * TABLE: job_requisition
 *
 * v1 SIMPLIFICATION: requisition and "job posting" are one entity. Split them
 * only if you later need several postings per approved requisition.
 *
 * STATUS COLUMN: stored as plain VARCHAR (not a MySQL native ENUM) so adding
 * a status later never needs an ALTER TABLE - see @JdbcTypeCode below.
 */
@Entity
@Table(name = "job_requisition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // REQ-2026-0001 - assigned right after the first save, once the id is known.
    @Column(name = "requisition_code", unique = true, length = 30)
    private String requisitionCode;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_type_id", nullable = false)
    private EmployeeType employeeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "openings", nullable = false)
    private Integer openings;

    @Column(name = "filled_count", nullable = false)
    @Builder.Default
    private Integer filledCount = 0;

    // Annual budget band (CTC). Offers above budget_max need a written exception reason.
    @Column(name = "budget_min", precision = 14, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 14, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "min_experience", precision = 4, scale = 1)
    private BigDecimal minExperience;

    @Column(name = "max_experience", precision = 4, scale = 1)
    private BigDecimal maxExperience;

    @Column(name = "job_description", length = 4000)
    private String jobDescription;

    @Column(name = "justification", length = 1000)
    private String justification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hiring_manager_id", nullable = false)
    private Employee hiringManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id")
    private Employee recruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private Employee requestedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @Column(name = "target_close_date")
    private LocalDate targetCloseDate;

    // Last reason given for hold / close / cancel / reject.
    @Column(name = "status_remarks", length = 500)
    private String statusRemarks;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<RequisitionApproval> approvals = new ArrayList<>();

    // Optimistic locking: two people can't silently overwrite each other's change.
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
