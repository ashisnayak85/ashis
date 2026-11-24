package com.onehealth.dto;

import com.onehealth.entity.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateEmployeeProfileRequest {
    private Gender gender;
    private LocalDate dob;
    private LocalDate dateOfJoining;
    private String permanentAddress;
    private String currentAddress;
}
