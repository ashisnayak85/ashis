package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

/** One line the doctor prescribed - medicine + quantity + dosage. No price - see PrescribedItem entity. */
@Getter
@Builder
public class PrescribedItemDTO {
    private Long medicineId;
    private String medicineName;
    private String unit;
    private String packLabel;
    private Integer prescribedQuantity;
    private String dosageInstructions;
}
