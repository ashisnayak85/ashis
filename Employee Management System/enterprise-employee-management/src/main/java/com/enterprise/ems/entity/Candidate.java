package com.enterprise.ems.entity;

import com.enterprise.ems.constant.CandidateSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * PURPOSE: A person being considered for hiring. DELIBERATELY NOT an Employee:
 * candidates must never appear in attendance, dashboards or exports. An
 * Employee is created only at the very end (Offer -> convert to employee).
 * TABLE: candidate
 *
 * PRIVACY: holds personal data and salary expectations. Access is limited in
 * the service layer (HiringAccessService). The resume lives in a private
 * folder served only through an authenticated endpoint - NOT in /uploads.
 * anonymise() support exists for retention / deletion requests.
 */
@Entity
@Table(name = "candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    // Always stored lower-case so the unique check is case-insensitive.
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "current_company", length = 150)
    private String currentCompany;

    @Column(name = "current_designation", length = 100)
    private String currentDesignation;

    @Column(name = "current_location", length = 100)
    private String currentLocation;

    @Column(name = "total_experience", precision = 4, scale = 1)
    private BigDecimal totalExperience;

    @Column(name = "current_ctc", precision = 14, scale = 2)
    private BigDecimal currentCtc;

    @Column(name = "expected_ctc", precision = 14, scale = 2)
    private BigDecimal expectedCtc;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "skills", length = 500)
    private String skills;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source", nullable = false, length = 30)
    @Builder.Default
    private CandidateSource source = CandidateSource.OTHER;

    // Set when source = AGENCY (kept as free text until vendor management exists).
    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    // Set when source = REFERRAL.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_id")
    private Employee referredBy;

    // --- Resume (stored outside the public /uploads folder) ---
    @Column(name = "resume_stored_name", length = 100)
    private String resumeStoredName;

    @Column(name = "resume_original_name", length = 255)
    private String resumeOriginalName;

    @Column(name = "resume_content_type", length = 100)
    private String resumeContentType;

    @Column(name = "resume_uploaded_at")
    private LocalDateTime resumeUploadedAt;

    // Consent to process personal data (DPDP-style). Required when a recruiter creates the candidate.
    @Column(name = "consent_given_at")
    private LocalDateTime consentGivenAt;

    // false once anonymised - the row is kept so application history stays intact.
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "anonymised_at")
    private LocalDateTime anonymisedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Employee createdBy;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFullName() {
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }
}
