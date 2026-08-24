package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ClinicSummaryDTO {
    private Long id;
    private String clinicName;
    private String address;
    private String city;
    private String pincode;
    private String phone;
    private Double latitude;
    private Double longitude;
    private boolean verified;
    private boolean active;
    private long doctorCount;
}
