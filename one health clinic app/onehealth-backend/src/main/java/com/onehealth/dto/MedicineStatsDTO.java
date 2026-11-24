package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Per-medicine rollup for the selected range - "which medicine sells the most / earns the most profit". */
@Getter
@Builder(toBuilder = true)
public class MedicineStatsDTO {
    private Long medicineId;
    private String medicineName;
    private long quantitySold;
    private BigDecimal revenue;
    private BigDecimal profit;
    private double profitMarginPercent; // profit / revenue * 100, for this medicine, this range
}
