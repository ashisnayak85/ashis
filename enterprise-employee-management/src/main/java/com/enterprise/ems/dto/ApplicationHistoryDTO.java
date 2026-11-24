package com.enterprise.ems.dto;

import com.enterprise.ems.constant.ApplicationStage;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationHistoryDTO {
    private Long id;
    private ApplicationStage fromStage;
    private ApplicationStage toStage;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;
}
