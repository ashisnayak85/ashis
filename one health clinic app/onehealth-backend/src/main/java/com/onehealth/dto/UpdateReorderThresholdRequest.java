package com.onehealth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Clinic-admin-facing: set this branch's low-stock alert threshold for one
 * medicine. Deliberately does NOT include margin/price - that's an
 * owner-only decision, see UpdateMarginRequest. This is purely an
 * operational setting (when to flag "running low"), not a pricing one.
 */
@Data
public class UpdateReorderThresholdRequest {
    @NotNull
    @Min(0)
    private Integer reorderThreshold;
}
