package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationClearanceDTO {

    private Long id;
    private Long departmentId;
    private String departmentName;
    private String status; // PENDING / CLEARED / BLOCKED
    private Long clearedByEmployeeId;
    private String clearedByName;
    private String remarks;
    private LocalDateTime clearedAt;
}
