package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per salary revision - appended, never overwritten, so a raise never
 * erases the record of what someone earned before it. "Current salary" for an
 * employee is the record with the latest effectiveFrom date that is on or
 * before today; if a future-dated revision exists (e.g. a raise agreed now but
 * effective next month), it simply doesn't count as "current" yet.
 *
 * Deliberately only reachable through OWNER-only endpoints (OwnerEmployeeController)
 * - never exposed on DoctorDTO/ClinicAdmin-facing responses that other roles
 * (patients, other staff) might see.
 */
@Entity
@Table(name = "salary_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_profile_id", nullable = false)
    private Long employeeProfileId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
