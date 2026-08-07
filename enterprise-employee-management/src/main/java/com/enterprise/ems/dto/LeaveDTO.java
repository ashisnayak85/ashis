package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveDTO {

    private Long id;

    @NotNull(message = "Employee is required")
    private Long employeeId;

    private String employeeName;

    @NotBlank(message = "Leave type is required")
    private String leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Size(max = 500)
    private String reason;

    private String status;

    private String approvedBy;
}
