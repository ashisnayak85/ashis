package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClinicDTO {
    private Long id;
    private String clinicName;
    private String address;
    private String city;
    private String pincode;
    private String phone;
    private Double latitude;
    private Double longitude;
    private boolean active;
    private String clinicAdminName;
    private Integer doctorCount;
}
