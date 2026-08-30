package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * organizationId is nullable ONLY for SUPER_ADMIN (the platform operator who
 * onboards new clinic-chain customers). Every other role belongs to exactly one
 * organization - this is the tenant-isolation boundary. Every query for a
 * non-super-admin role MUST filter by this organizationId; see the OrgScoped
 * convention in the service layer (checked explicitly in each service method
 * rather than a global Hibernate filter, so it stays visible in code review).
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hash

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Null only for SUPER_ADMIN.
    @Column(name = "organization_id")
    private Long organizationId;

    // Display name for roles that have no separate profile table (OWNER,
    // SUPER_ADMIN). CLINIC_ADMIN/DOCTOR/PATIENT keep their name on their own
    // profile entity instead, same convention as the marketplace project.
    @Column(length = 150)
    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Role {
        SUPER_ADMIN, OWNER, CLINIC_ADMIN, DOCTOR, PATIENT
    }
}
