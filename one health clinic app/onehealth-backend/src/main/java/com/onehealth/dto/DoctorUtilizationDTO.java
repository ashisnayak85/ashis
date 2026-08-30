package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorUtilizationDTO {
    private Long doctorId;
    private String doctorName;
    private Long clinicId;
    private String clinicName;
    private long slotsTotal;
    private long slotsBooked;
    private double utilizationRatePercent;
}
