package com.onehealth.dto;

import com.onehealth.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterPatientRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    // Which clinic-chain the patient is signing up under, e.g. "one-health".
    @NotBlank
    private String organizationSlug;

    private LocalDate dob;
    private Gender gender;
}
