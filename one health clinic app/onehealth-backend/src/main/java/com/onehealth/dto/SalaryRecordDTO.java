package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SalaryRecordDTO {
    private Long id;
    private BigDecimal amount;
    private LocalDate effectiveFrom;
    private LocalDateTime createdAt;
}
