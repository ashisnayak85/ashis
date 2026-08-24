package com.doctorapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents the relationship "this doctor practices at this clinic". This is the
 * real many-to-many link between {@link Doctor} and {@link Clinic}: one doctor can
 * have many APPROVED associations (many clinics), and one clinic can have many
 * APPROVED associations (many doctors).
 *
 * Either side can initiate the relationship, but the OTHER side must approve it -
 * a doctor can't unilaterally attach themselves to a clinic's public listing, and
 * a clinic can't unilaterally claim a doctor works for them. See initiatedBy.
 *
 * {@link DoctorAvailability} rows (which day/time a doctor sees patients at a
 * clinic) should only be created once the matching association here is APPROVED.
 */
@Entity
@Table(
    name = "doctor_clinic_associations",
    uniqueConstraints = @UniqueConstraint(name = "uk_doctor_clinic", columnNames = {"doctor_id", "clinic_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorClinicAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InitiatedBy initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum InitiatedBy {
        DOCTOR, CLINIC
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, REMOVED
    }
}
