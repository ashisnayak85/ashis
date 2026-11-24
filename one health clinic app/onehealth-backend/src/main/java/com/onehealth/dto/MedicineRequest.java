package com.onehealth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Owner-facing: add/update an org-wide medicine catalog entry.
 * Deliberately carries NO price field - cost/MRP/margin are all set later,
 * per-branch, at dispatch time (see DispatchStockRequest) and by the owner's
 * margin control (see UpdateMarginRequest). A medicine's catalog identity
 * should never require a pricing decision to create.
 */
@Data
public class MedicineRequest {
    @NotBlank
    private String name;

    private String genericName;
    private String manufacturer;

    @NotBlank
    private String unit;

    private String category;

    private boolean scheduledDrug;

    // Optional: how many base `unit`s make up one physical pack (e.g. 10
    // tablets per strip). Leave null for medicines dispensed one unit at a
    // time (bottles, tubes). If set, packLabel should be set too.
    @Min(value = 2, message = "Pack size must be at least 2 - use no pack size for single-unit medicines")
    private Integer packSize;

    // Display label for the pack, e.g. "strip", "box". Required if packSize is set.
    private String packLabel;
}
