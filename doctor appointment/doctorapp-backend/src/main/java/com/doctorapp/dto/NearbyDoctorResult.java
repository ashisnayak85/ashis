package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Projection returned by the native "nearby" query. Native queries can't return
 * entities with lazy collections cleanly, so this flat DTO carries exactly the
 * fields the doctor-search screen needs, including the computed distance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDoctorResult {
    private Long doctorId;
    private String doctorName;
    private String qualification;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private String profileImageUrl;
    private Long clinicId;
    private String clinicName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}
