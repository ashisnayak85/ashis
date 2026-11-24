package com.enterprise.ems.dto;

import com.enterprise.ems.constant.CandidateSource;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * PURPOSE: Request AND response shape for a candidate.
 * currentCtc / expectedCtc are returned as null to anyone who is not hiring staff.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Pattern(regexp = "^([6-9]\\d{9})?$", message = "Enter a valid 10-digit Indian mobile number")
    private String mobile;

    @Size(max = 150)
    private String currentCompany;
    @Size(max = 100)
    private String currentDesignation;
    @Size(max = 100)
    private String currentLocation;

    @DecimalMin(value = "0.0", message = "Experience cannot be negative")
    @DecimalMax(value = "60.0", message = "Experience looks invalid")
    private BigDecimal totalExperience;

    @DecimalMin(value = "0.0", message = "CTC cannot be negative")
    private BigDecimal currentCtc;
    @DecimalMin(value = "0.0", message = "CTC cannot be negative")
    private BigDecimal expectedCtc;

    @Min(value = 0, message = "Notice period cannot be negative")
    @Max(value = 365, message = "Notice period looks too long")
    private Integer noticePeriodDays;

    @Size(max = 500, message = "Skills must not exceed 500 characters")
    private String skills;

    private CandidateSource source;

    @Size(max = 150)
    private String vendorName;

    private Long referredById;
    private String referredByName;                       // response only

    // Input on create: recruiter confirms the candidate agreed to data processing.
    private Boolean consentGiven;
    private LocalDateTime consentGivenAt;                // response only

    private boolean resumeAvailable;                     // response only
    private String resumeFileName;                       // response only
    private LocalDateTime resumeUploadedAt;              // response only

    private Boolean active;                              // response only

    // If a person with this email already exists as an Employee (possible rehire) - response only.
    private Long existingEmployeeId;

    private LocalDateTime createdAt;                     // response only
}
