package com.enterprise.ems.entity;

import com.enterprise.ems.constant.OfferStatus;
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
 * PURPOSE: A job offer to a candidate for one application. Goes through the
 * admin-configured OFFER approval workflow, is sent, accepted/declined/expired,
 * and finally converted into an Employee.
 * TABLE: job_offer
 *
 * An application may have several offers over time (e.g. a revised offer after
 * the first was revoked) but only ONE open offer at a time - see OfferStatus.OPEN_STATUSES.
 */
@Entity
@Table(name = "job_offer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    // Annual CTC. Converted to Employee.salary on hire (see OfferServiceImpl#convertToEmployee).
    @Column(name = "annual_ctc", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualCtc;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // Required when annualCtc is above the requisition's budget_max.
    @Column(name = "budget_exception_reason", length = 500)
    private String budgetExceptionReason;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // When the candidate's answer (accept / decline) was recorded.
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Decline / revoke reason.
    @Column(name = "closing_reason", length = 500)
    private String closingReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_employee_id")
    private Employee convertedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Employee createdBy;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<OfferApproval> approvals = new ArrayList<>();

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
