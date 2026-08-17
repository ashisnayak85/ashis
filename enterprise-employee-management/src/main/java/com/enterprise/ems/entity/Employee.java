package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE: Core business entity - Employee records
 * TABLE: employee
 * WHY EXISTS: Central entity for HR operations (CRUD, attendance, leave)
 */
@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "designation", length = 100)
    private String designation;

    // --- Qualification & Experience ---
    @Column(name = "qualification", length = 100)
    private String qualification;

    @Column(name = "year_of_passing")
    private Integer yearOfPassing;

    // In years, e.g. 3.5 for 3 years 6 months
    @Column(name = "total_experience_years", precision = 4, scale = 1)
    private BigDecimal totalExperience;

    // SINGLE / MARRIED / DIVORCED / WIDOWED - plain String (not enum) to match
    // the rest of this codebase's convention of validating via @Pattern on the DTO.
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    @Column(name = "aadhar_number", unique = true, length = 12)
    private String aadharNumber;

    // MONTHLY / DAILY / HOURLY / ANNUAL - basis payroll uses to compute salary.
    @Column(name = "salary_calculation_basis", length = 20)
    private String salaryCalculationBasis;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine", column = @Column(name = "present_address_line")),
            @AttributeOverride(name = "cityOrDistrict", column = @Column(name = "present_city_district")),
            @AttributeOverride(name = "state", column = @Column(name = "present_state")),
            @AttributeOverride(name = "pincode", column = @Column(name = "present_pincode")),
    })
    private Address presentAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine", column = @Column(name = "permanent_address_line")),
            @AttributeOverride(name = "cityOrDistrict", column = @Column(name = "permanent_city_district")),
            @AttributeOverride(name = "state", column = @Column(name = "permanent_state")),
            @AttributeOverride(name = "pincode", column = @Column(name = "permanent_pincode")),
    })
    private Address permanentAddress;

    @Embedded
    private BankDetails bankDetails;

    @Embedded
    private StatutoryInfo statutoryInfo;

    // Points at file_upload.id for this employee's uploaded qualification
    // certificate PDF. Kept as a plain Long (not a JPA relationship) to match
    // how FileUpload already links loosely via entityType/entityId elsewhere.
    @Column(name = "qualification_certificate_file_id")
    private Long qualificationCertificateFileId;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Independent from Department on purpose (see Location's class comment) -
    // nullable for now so existing employee rows don't break; tighten to
    // NOT NULL once existing data has been backfilled with a location.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Attendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LeaveMaster> leaves = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Transient - not persisted, computed at runtime
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
