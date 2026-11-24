package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PrescriptionItemDTO {
    private Long medicineId;
    private String medicineName;
    private Integer quantity;
    private BigDecimal costPriceAtSale;
    private BigDecimal sellingPriceAtSale;
    private BigDecimal profitAmount;
    private String dosageInstructions;
}
