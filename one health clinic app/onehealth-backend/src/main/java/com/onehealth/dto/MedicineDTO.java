package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicineDTO {
    private Long id;
    private String name;
    private String genericName;
    private String manufacturer;
    private String unit;
    private String category;
    private boolean scheduledDrug;
    private Integer packSize;
    private String packLabel;
    private boolean active;
}
