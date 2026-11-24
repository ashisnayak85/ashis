package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * "This doctor works at this branch" - a direct assignment made by the owner
 * (or a clinic admin, if the owner delegates that), with NO approval workflow.
 * This is the key simplification vs. the public marketplace project's
 * DoctorClinicAssociation: since both the doctor and the clinic belong to the
 * same trusted organization, there's no need for either side to "approve" the
 * relationship - the owner just assigns it.
 */
@Entity
@Table(
    name = "doctor_clinic_assignments",
    uniqueConstraints = @UniqueConstraint(name = "uk_doctor_clinic", columnNames = {"doctor_id", "clinic_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorClinicAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Builder.Default
    private boolean active = true;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }
}
