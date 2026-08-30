package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The paying customer - a clinic chain (e.g. "One Health"). Everything else
 * (clinics, doctors, clinic admins, patients, appointments) hangs off an
 * organization. This is what makes the platform resellable to a second clinic
 * chain later without a schema rewrite: add a row here, everything else already
 * has an organization_id to scope into.
 *
 * For a single-customer deployment, exactly one row exists here and the app
 * behaves like a plain single-org system - the multi-tenancy is invisible to
 * that customer, it's just future-proofing.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    // Short unique code used in patient-facing URLs / registration
    // (e.g. onehealth.app/o/one-health) and to route a login to the right tenant.
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(length = 20)
    private String supportPhone;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
