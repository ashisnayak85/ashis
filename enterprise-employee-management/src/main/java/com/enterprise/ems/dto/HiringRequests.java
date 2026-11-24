package com.enterprise.ems.dto;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.constant.InterviewRecommendation;
import com.enterprise.ems.constant.InterviewStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * PURPOSE: Small request bodies for hiring actions, kept together as records
 * (same style as UpdateRoleRequest elsewhere) instead of ten tiny files.
 */
public final class HiringRequests {

    private HiringRequests() {}

    public enum RequisitionAction { OPEN, HOLD, RESUME, CLOSE, CANCEL }

    public record RemarksRequest(@Size(max = 500, message = "Remarks must not exceed 500 characters") String remarks) {}

    public record ReasonRequest(@Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {}

    public record StatusActionRequest(
            @NotNull(message = "Action is required") RequisitionAction action,
            @Size(max = 500, message = "Remarks must not exceed 500 characters") String remarks) {}

    public record AssignRecruiterRequest(@NotNull(message = "Recruiter is required") Long recruiterId) {}

    public record ApplyRequest(
            @NotNull(message = "Candidate is required") Long candidateId,
            @NotNull(message = "Requisition is required") Long requisitionId) {}

    public record MoveRequest(
            @NotNull(message = "Target stage is required") ApplicationStage toStage,
            @Size(max = 500, message = "Remarks must not exceed 500 characters") String remarks) {}

    public record InterviewStatusRequest(@NotNull(message = "Status is required") InterviewStatus status) {}

    public record FeedbackRequest(
            @NotNull(message = "Rating is required")
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5") Integer rating,
            @NotNull(message = "Recommendation is required") InterviewRecommendation recommendation,
            @Size(max = 2000, message = "Comments must not exceed 2000 characters") String comments) {}

    // Everything optional: omitted values are taken from the candidate / offer / requisition.
    public record ConvertRequest(
            @Pattern(regexp = "^([A-Z0-9]{1,20})?$", message = "Employee code must be uppercase alphanumeric") String employeeCode,
            LocalDate dateOfJoining,
            LocalDate dateOfBirth,
            @Pattern(regexp = "^(MALE|FEMALE|OTHER)?$", message = "Invalid gender") String gender,
            Long reportingManagerId,
            Long locationId,
            @DecimalMin(value = "0.0", message = "Salary cannot be negative") BigDecimal salary,
            @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters") String firstName,
            @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters") String lastName) {}
}
