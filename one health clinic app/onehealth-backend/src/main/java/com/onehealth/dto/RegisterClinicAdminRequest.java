package com.onehealth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** OWNER-only: create the front-desk login for one clinic. */
@Data
public class RegisterClinicAdminRequest {
    @NotNull
    private Long clinicId;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String phone;
}
