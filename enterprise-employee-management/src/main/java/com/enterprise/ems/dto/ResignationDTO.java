package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationDTO {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeCode;

    private String reason;

    private LocalDate requestedOn;
    private Integer noticePeriodDays;
    private LocalDate expectedLastWorkingDay;
    private LocalDate actualLastWorkingDay;
    private String overrideReason;

    private String status;

    private List<ResignationApprovalDTO> approvals;
    private List<ResignationClearanceDTO> clearances;
    private List<ResignationAssetDTO> assets;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
