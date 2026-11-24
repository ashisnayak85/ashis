package com.enterprise.ems.dto;

import com.enterprise.ems.constant.RequisitionStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
 * PURPOSE: Request AND response shape for a job requisition (same approach as ResignationDTO).
 * Fields marked "response only" are ignored on input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionDTO {

    private Long id;
    private String requisitionCode;                      // response only

    @NotBlank(message = "Job title is required")
    @Size(max = 150, message = "Job title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Department is required")
    private Long departmentId;
    private String departmentName;                       // response only

    @NotNull(message = "Designation is required")
    private Long designationId;
    private String designationName;                      // response only

    @NotNull(message = "Employee type is required")
    private Long employeeTypeId;
    private String employeeTypeName;                     // response only

    private Long locationId;
    private String locationName;                         // response only

    @NotNull(message = "Number of openings is required")
    @Min(value = 1, message = "At least 1 opening is required")
    @Max(value = 500, message = "Openings looks too high")
    private Integer openings;
    private Integer filledCount;                         // response only

    @DecimalMin(value = "0.0", message = "Budget cannot be negative")
    private BigDecimal budgetMin;
    @DecimalMin(value = "0.0", message = "Budget cannot be negative")
    private BigDecimal budgetMax;

    @DecimalMin(value = "0.0", message = "Experience cannot be negative")
    private BigDecimal minExperience;
    @DecimalMin(value = "0.0", message = "Experience cannot be negative")
    private BigDecimal maxExperience;

    @Size(max = 4000, message = "Job description must not exceed 4000 characters")
    private String jobDescription;

    @Size(max = 1000, message = "Justification must not exceed 1000 characters")
    private String justification;

    // Defaults to the person raising the requisition when omitted.
    private Long hiringManagerId;
    private String hiringManagerName;                    // response only

    private Long recruiterId;                            // set by hiring staff via assign-recruiter
    private String recruiterName;                        // response only

    private Long requestedById;                          // response only
    private String requestedByName;                      // response only

    private RequisitionStatus status;                    // response only
    private LocalDate targetCloseDate;
    private String statusRemarks;                        // response only

    private List<ApprovalDTO> approvals;                 // response only
    private long applicationCount;                       // response only

    // UI hints computed per viewer (response only) - so the screen can show/hide buttons.
    private boolean viewerCanApprove;
    private boolean viewerCanEdit;
    private boolean viewerCanManage;

    private LocalDateTime createdAt;
}
