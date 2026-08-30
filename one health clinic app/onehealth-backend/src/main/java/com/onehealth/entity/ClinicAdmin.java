package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The "clinic authority" for exactly one branch - front-desk staff account that
 * can book walk-ins, view that branch's schedule/appointments, and manage the
 * doctors assigned there. One clinic has one ClinicAdmin account by default
 * (uniqueConstraint on clinic_id); if a branch needs more than one staff login
 * later, relax this to allow multiple ClinicAdmin rows per clinic.
 */
@Entity
@Table(name = "clinic_admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "clinic_id", nullable = false, unique = true)
    private Long clinicId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
