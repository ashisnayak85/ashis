package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * PURPOSE: Data Transfer Object for Employee - decouples API/UI from Entity
 * WHY DTO: Never expose JPA entities directly (lazy loading, security risks)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "Employee code is required")
    @Size(max = 20, message = "Employee code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Employee code must be uppercase alphanumeric")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    private LocalDate dateOfBirth;

    @NotNull(message = "Date of joining is required")
    private LocalDate dateOfJoining;

    @Min(value = 0, message = "Salary cannot be negative")
    private BigDecimal salary;

    private String designation;

    // --- Qualification & Experience ---
    private String qualification;

    @Min(value = 1950, message = "Year of passing looks invalid")
    @Max(value = 2100, message = "Year of passing looks invalid")
    private Integer yearOfPassing;

    @DecimalMin(value = "0.0", message = "Total experience cannot be negative")
    private BigDecimal totalExperience;

    @Pattern(regexp = "^(SINGLE|MARRIED|DIVORCED|WIDOWED)?$", message = "Invalid marital status")
    private String maritalStatus;

    @Pattern(regexp = "^(\\d{12})?$", message = "Aadhar number must be exactly 12 digits")
    private String aadharNumber;

    @Pattern(regexp = "^(MONTHLY|DAILY|HOURLY|ANNUAL)?$", message = "Invalid salary calculation basis")
    private String salaryCalculationBasis;

    // --- Present Address ---
    private String presentAddressLine;
    private String presentCityDistrict;
    private String presentState;
    @Pattern(regexp = "^(\\d{6})?$", message = "Present address pincode must be 6 digits")
    private String presentPincode;

    // --- Permanent Address ---
    private String permanentAddressLine;
    private String permanentCityDistrict;
    private String permanentState;
    @Pattern(regexp = "^(\\d{6})?$", message = "Permanent address pincode must be 6 digits")
    private String permanentPincode;

    // --- Bank Details ---
    private String bankName;
    private String bankAccountNumber;
    @Pattern(regexp = "^([A-Z]{4}0[A-Z0-9]{6})?$", message = "Invalid IFSC code format")
    private String bankIfscCode;

    // --- Statutory Information ---
    private Boolean pfApplicable;
    private String pfNumber;
    private String uanNumber;
    private Boolean restrictPf;
    private Boolean zeroPension;
    private Boolean zeroPt;
    private Boolean esiApplicable;
    private String esiNumber;
    private Boolean esiDispensation;

    // Set by the frontend after a successful upload to /api/files/upload
    // (entityType=EMPLOYEE_QUALIFICATION_CERTIFICATE). Not itself a file field -
    // multipart uploads are handled separately from this JSON payload.
    private Long qualificationCertificateFileId;

    private String profilePhoto;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;

    // Optional for now (unlike departmentId) - existing employees predate this
    // field. Make @NotNull once historical data has been backfilled with a location.
    private Long locationId;

    private String locationName;

    private Boolean active;
}
