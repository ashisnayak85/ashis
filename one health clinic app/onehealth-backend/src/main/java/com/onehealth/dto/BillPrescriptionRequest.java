package com.onehealth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Clinic-admin-facing: create the bill for a prescription. This is the step
 * that actually deducts stock and charges the patient - see
 * Prescription.PrescriptionStatus javadoc. Any prescribed line the admin
 * omits here simply isn't billed (e.g. the patient chose not to buy it).
 */
@Data
public class BillPrescriptionRequest {
    @NotEmpty(message = "Bill at least one medicine")
    @Valid
    private List<BillLineRequest> items;
}
