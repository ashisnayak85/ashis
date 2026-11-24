package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * No "verified" flag (unlike the marketplace project) - the owner adds their own
 * doctors directly. Doctors are org-scoped, and which clinic(s) a doctor works at
 * is modeled separately via DoctorClinicAssignment (a doctor can be assigned to
 * more than one branch of the same org).
 */
@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 150)
    private String name;

    // Replaced the earlier free-text specialization field with a proper
    // many-to-many against the org's Specialization master list (owner-managed,
    // multi-select on the doctor form) - see Specialization entity javadoc for why.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "doctor_specializations",
        joinColumns = @JoinColumn(name = "doctor_id"),
        inverseJoinColumns = @JoinColumn(name = "specialization_id")
    )
    @Builder.Default
    private java.util.Set<Specialization> specializations = new java.util.HashSet<>();

    @Column(length = 150)
    private String qualification;

    private Integer experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate dob;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    private String profileImageUrl;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
