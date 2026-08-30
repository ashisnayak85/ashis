package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * userId is NULLABLE - unlike the marketplace project, a patient here doesn't
 * necessarily have a login. A walk-in who never used the app has their record
 * created directly by the clinic admin (name + phone), no account needed. If
 * that same person later self-registers in the app, link their existing Patient
 * row to the new User by phone match (see PatientService.linkOrCreateForSelfSignup)
 * rather than creating a duplicate patient record.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    private java.time.LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    // Set when a clinic admin created this record for a walk-in, so the
    // dashboard/audit trail can show who registered the patient.
    @Column(name = "created_by_clinic_admin_id")
    private Long createdByClinicAdminId;
}
