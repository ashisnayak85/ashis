package com.onehealth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateDoctorSpecializationsRequest {
    private List<Long> specializationIds;
}
