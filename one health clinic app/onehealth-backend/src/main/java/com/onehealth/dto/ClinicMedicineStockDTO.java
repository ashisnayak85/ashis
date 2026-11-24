package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Current stock position for one medicine at one clinic, WITH cost/profit
 * data - the owner's stock management screen's row. OWNER-ONLY: never
 * returned to a clinic admin or doctor (they get ClinicStockViewDTO instead,
 * which omits currentCostPrice and marginPercent so cost can't be derived).
 */
@Getter
@Builder(toBuilder = true)
public class ClinicMedicineStockDTO {
    // Populated only when this row is shown in a cross-clinic context (e.g. the
    // owner dashboard's low-stock alert list) - null in a clinic admin's own
    // single-clinic stock screen, where it would be redundant.
    private Long clinicId;
    private String clinicName;

    private Long medicineId;
    private String medicineName;
    private String unit;
    private Integer packSize;
    private String packLabel;
    private boolean scheduledDrug;

    // Set by the OWNER only - see MedicineService.updateMargin.
    private BigDecimal marginPercent;
    private Integer quantityOnHand;
    private Integer reorderThreshold;
    private boolean lowStock;

    // Computed selling price at the current margin, off the earliest-expiring
    // batch's cost price - shown as a preview; the actual sale snapshots
    // whichever batch FEFO picks at dispense time, which may differ slightly
    // once that batch runs out and the next one (different cost) is used.
    private BigDecimal currentCostPrice;
    private BigDecimal currentMrp;
    private BigDecimal currentSellingPrice;
    private java.time.LocalDate nearestExpiryDate;
}
