package com.onehealth.dto;

import com.onehealth.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** OWNER-only: create a doctor login and (optionally) assign them to branches immediately. */
@Data
public class RegisterDoctorRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    // Multi-select against the org's Specialization master list - a doctor can
    // have more than one (e.g. a physician who also does sports medicine).
    private List<Long> specializationIds;
    private String qualification;
    private Integer experienceYears;
    private Gender gender;
    private LocalDate dob;
    private BigDecimal consultationFee;

    // Clinic IDs to assign this doctor to right away (optional).
    private List<Long> clinicIds;
}
