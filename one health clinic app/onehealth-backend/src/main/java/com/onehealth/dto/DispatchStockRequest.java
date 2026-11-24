package com.onehealth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Owner-facing: dispatch (send) a quantity of medicine from HQ to one clinic.
 * This is deliberately an OWNER action, not a clinic-admin one - see
 * StockBatch's class javadoc for why centralizing this is what makes
 * dispatched-vs-sold reconciliation meaningful.
 *
 * Quantity can be entered EITHER as a number of packs (e.g. "5 strips") when
 * the medicine has a packSize, OR directly as base units (e.g. "37 loose
 * tablets") - exactly one of packsReceived/quantityReceived should be set.
 * Whichever is given, stock is always stored/dispensed in base units - see
 * Medicine.packSize javadoc.
 */
@Data
public class DispatchStockRequest {
    @NotNull
    private Long clinicId;

    @NotNull
    private Long medicineId;

    private String batchNumber;

    // Cost PER BASE UNIT (e.g. per tablet), never per pack.
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than zero")
    private BigDecimal costPrice;

    // MRP PER BASE UNIT (e.g. per tablet) - required, since this is now the
    // only source of MRP (no catalog-level price).
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "MRP must be greater than zero")
    private BigDecimal mrp;

    // Number of whole packs (e.g. strips) being dispatched - use this when the
    // medicine has a packSize. Converted to base units server-side.
    @Min(value = 1, message = "Packs dispatched must be at least 1")
    private Integer packsReceived;

    // Quantity directly in base units (e.g. loose tablets/ml) - use this when
    // the medicine has no packSize, or to dispatch a loose/partial amount.
    @Min(value = 1, message = "Quantity dispatched must be at least 1")
    private Integer quantityReceived;

    private LocalDate expiryDate;
    private LocalDate receivedDate;
}
