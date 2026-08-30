package com.onehealth.dto;

import com.onehealth.entity.Gender;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DoctorDTO {
    private Long id;
    private String name;
    private List<SpecializationDTO> specializations;
    private String qualification;
    private Integer experienceYears;
    private Gender gender;
    private BigDecimal consultationFee;
    private boolean active;
    private List<ClinicDTO> assignedClinics;
}
