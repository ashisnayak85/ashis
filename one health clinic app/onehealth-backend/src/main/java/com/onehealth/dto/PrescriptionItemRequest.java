package com.onehealth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** One medicine the doctor is prescribing - quantity here is a clinical recommendation, not a chargeable amount. */
@Data
public class PrescriptionItemRequest {
    @NotNull
    private Long medicineId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String dosageInstructions;
}
