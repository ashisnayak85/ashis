package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DoctorProfileDTO {
    private Long id;
    private String name;
    private String qualification;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private String profileImageUrl;
    private boolean verified;
    private List<String> specializations;
    private List<ClinicSummary> clinics;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ClinicSummary {
        private Long id;
        private String clinicName;
        private String address;
        private Double latitude;
        private Double longitude;
    }
}
