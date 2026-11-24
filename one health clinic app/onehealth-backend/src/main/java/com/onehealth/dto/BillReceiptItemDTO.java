package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** One billed line on a receipt - selling price only, no cost, no profit. */
@Getter
@Builder
public class BillReceiptItemDTO {
    private Long medicineId;
    private String medicineName;
    private String unit;
    private Integer quantityBilled;
    private BigDecimal sellingPricePerUnit;
    private BigDecimal lineTotal;
}
