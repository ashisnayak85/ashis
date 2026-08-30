package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EmployeeListItemDTO {
    private Long userId;
    private String name;
    private String role;
    private String clinicSummary;
    private boolean profileComplete;
    private BigDecimal currentSalary;
}
