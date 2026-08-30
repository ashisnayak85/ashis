package com.onehealth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClinicRequest {
    @NotBlank
    private String clinicName;

    @NotBlank
    private String address;

    private String city;
    private String pincode;
    private String phone;
    private Double latitude;
    private Double longitude;
}
