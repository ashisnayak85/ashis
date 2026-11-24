package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One prescription written against a single appointment. Two-step lifecycle,
 * matching how the clinic actually operates:
 *
 *   1. PRESCRIBED - the doctor writes what medicines/quantities the patient
 *      needs (see PrescribedItem). No pricing exists yet and no stock is
 *      touched - a doctor's job is clinical, not commercial.
 *   2. BILLED - the clinic admin, at the counter, decides how much of each
 *      prescribed item to actually dispense (which may be LESS than
 *      prescribed - e.g. a patient can only afford 6 of the 10 prescribed
 *      tablets) and creates the bill. THIS is the moment stock is deducted
 *      and price/profit gets computed and snapshotted (see PrescriptionItem).
 *
 * Holds no pricing itself - that only exists once BILLED, and lives per-line
 * in PrescriptionItem.
 */
@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    public enum PrescriptionStatus {
        PRESCRIBED, BILLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status = PrescriptionStatus.PRESCRIBED;

    // Which clinic-admin user billed this, and when - audit trail for who
    // actually decided the chargeable quantities/collected payment.
    @Column(name = "billed_by_admin_id")
    private Long billedByAdminId;

    @Column(name = "billed_at")
    private LocalDateTime billedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
