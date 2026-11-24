package com.enterprise.ems.dto;

import com.enterprise.ems.constant.OfferStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/* Request AND response shape for a job offer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferDTO {

    private Long id;

    private Long applicationId;                          // response only (taken from the URL on create)
    private Long candidateId;                            // response only
    private String candidateName;                        // response only
    private Long requisitionId;                          // response only
    private String requisitionCode;                      // response only
    private String requisitionTitle;                     // response only

    @NotNull(message = "Designation is required")
    private Long designationId;
    private String designationName;                      // response only

    @NotNull(message = "Annual CTC is required")
    @DecimalMin(value = "1.0", message = "Annual CTC must be greater than zero")
    private BigDecimal annualCtc;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotNull(message = "Offer expiry date is required")
    private LocalDate expiryDate;

    @Size(max = 500, message = "Budget exception reason must not exceed 500 characters")
    private String budgetExceptionReason;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    private OfferStatus status;                          // response only
    private LocalDateTime sentAt;                        // response only
    private LocalDateTime respondedAt;                   // response only
    private String closingReason;                        // response only
    private Long convertedEmployeeId;                    // response only

    private BigDecimal requisitionBudgetMin;             // response only (hint for the form)
    private BigDecimal requisitionBudgetMax;             // response only
    private boolean aboveBudget;                         // response only

    private List<ApprovalDTO> approvals;                 // response only
    private boolean viewerCanApprove;                    // response only

    private LocalDateTime createdAt;
}
