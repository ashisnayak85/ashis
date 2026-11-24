package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * HR fields shared by any staff member (ClinicAdmin or Doctor today) - modeled
 * as a separate entity keyed by userId rather than adding these columns
 * directly onto ClinicAdmin/Doctor. Two reasons:
 *  1. Avoids duplicating the same HR fields across every future staff type
 *     (e.g. a Receptionist or Nurse role later) - they'd all just get an
 *     EmployeeProfile row too, keyed by their own userId.
 *  2. Keeps HR data structurally separate from operational profile data, which
 *     makes it easy to reason about who can see what - see SalaryRecord for the
 *     more sensitive half of this.
 *
 * Not every ClinicAdmin/Doctor will have a row here immediately - the owner
 * fills this in as HR data becomes available, it's not required at signup.
 */
@Entity
@Table(name = "employee_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate dob;

    private LocalDate dateOfJoining;

    @Column(length = 300)
    private String permanentAddress;

    @Column(length = 300)
    private String currentAddress;
}
