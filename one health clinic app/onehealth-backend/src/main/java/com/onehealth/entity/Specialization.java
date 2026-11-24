package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Org-managed master list of specializations (e.g. "Cardiology", "Pediatrics"),
 * replacing the earlier free-text field on Doctor. The owner adds/edits/
 * deactivates these; doctors are then multi-selected against this list rather
 * than typing their own text - keeps the data clean for reporting (no
 * "Cardiologist" vs "cardiologist" vs "Cardio" fragmentation).
 */
@Entity
@Table(
    name = "specializations",
    uniqueConstraints = @UniqueConstraint(name = "uk_org_specialization_name", columnNames = {"organization_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Builder.Default
    private boolean active = true;
}
