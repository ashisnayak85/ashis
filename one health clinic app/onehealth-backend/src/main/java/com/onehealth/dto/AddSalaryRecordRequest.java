package com.onehealth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddSalaryRecordRequest {
    @NotNull
    @DecimalMin(value = "0.0", message = "Salary amount can't be negative.")
    private BigDecimal amount;

    @NotNull
    private LocalDate effectiveFrom;
}
