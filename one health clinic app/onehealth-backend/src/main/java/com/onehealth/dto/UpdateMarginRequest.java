package com.onehealth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * OWNER-facing ONLY: set the chargeable margin for one medicine at one
 * branch. A clinic admin cannot call this - see MedicineService.updateMargin
 * and ClinicMedicineStock's class javadoc for why pricing authority is
 * centralized at the owner.
 */
@Data
public class UpdateMarginRequest {
    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "500.0")
    private BigDecimal marginPercent;
}
