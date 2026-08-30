package com.onehealth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddSalaryRecordRequest {
    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDate effectiveFrom;
}
