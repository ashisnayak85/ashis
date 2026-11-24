package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationApprovalDTO {

    private Long id;
    private Long stageId;
    private String stageName;
    private Integer sequenceOrder;
    private Long approverEmployeeId;
    private String approverName;
    private String status; // PENDING / APPROVED / REJECTED / SKIPPED
    private String remarks;
    private LocalDateTime actedAt;
}
