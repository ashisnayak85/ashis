package com.enterprise.ems.dto;

import com.enterprise.ems.constant.ApprovalStatus;
import lombok.*;

import java.time.LocalDateTime;

/* One approval stage as shown on a requisition or offer (same shape for both). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDTO {
    private Long id;
    private String stageName;
    private Integer sequenceOrder;
    private Long approverId;
    private String approverName;
    private ApprovalStatus status;
    private String remarks;
    private LocalDateTime actedAt;
}
