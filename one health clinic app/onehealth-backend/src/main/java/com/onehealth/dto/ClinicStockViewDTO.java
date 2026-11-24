package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Current stock position for one medicine at one clinic, WITHOUT any cost or
 * margin data - what a clinic admin or doctor is allowed to see. Only the
 * selling price is exposed (what the patient is actually charged); cost
 * price and margin percent are deliberately absent so cost can't be
 * back-calculated from selling price + margin. See ClinicMedicineStockDTO
 * for the owner-only equivalent with full pricing.
 */
@Getter
@Builder
public class ClinicStockViewDTO {
    private Long medicineId;
    private String medicineName;
    private String unit;
    private Integer packSize;
    private String packLabel;
    private boolean scheduledDrug;

    private Integer quantityOnHand;
    private Integer reorderThreshold;
    private boolean lowStock;

    // The only price shown - what a patient pays per base unit. No cost, no margin.
    private BigDecimal sellingPrice;
    private LocalDate nearestExpiryDate;
}
