package com.onehealth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDoctorRequest {
    @NotNull
    private Long doctorId;

    @NotNull
    private Long clinicId;
}
